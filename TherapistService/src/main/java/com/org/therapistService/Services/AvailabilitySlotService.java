package com.org.therapistService.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.events.TherapistAppointment.AppointmentStatus;
import com.org.events.TherapistAvailability.AvailabilityEvent;
import com.org.events.TherapistAvailability.AvailabilitySlotsDeletedEvent;
import com.org.events.TherapistAvailability.AvailabilitySlotsGeneratedEvent;
import com.org.events.TherapistAvailability.Slot;
import com.org.therapistService.Entity.TherapistAvailability;
import com.org.therapistService.Entity.TherapistAvailabilityOverrides;
import com.org.therapistService.Entity.TherapistAvailabilityRules;
import com.org.therapistService.Entity.TherapistServices;
import com.org.therapistService.Repository.AppointmentProjectionRepository;
import com.org.therapistService.Repository.TherapistAvailabilityOverridesRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;
import com.org.therapistService.Repository.TherapistServicesRepository;

import jakarta.transaction.Transactional;

// <<< DELIVERY-MODE-REPLICATION: removed SlotDeliveryOptionRepository and TherapyDeliveryModeRepository.
// Slots are mode-agnostic. Mode validation and pricing happen on AppointmentService side
// via its local THERAPY_DELIVERY_MODES projection. No SLOT_DELIVERY_OPTIONS table anywhere. >>>
@Service
public class AvailabilitySlotService {

	@Autowired
	private TherapistAvailabilityRulesRepository therapistAvailabilityRulesRepository;

	@Autowired
	private TherapistAvailabilityOverridesRepository therapistAvailabilityOverridesRepository;

	@Autowired
	private TherapistAvailabilityRepository therapistAvailabilityRepository;

	@Autowired
	private TherapistServicesRepository therapistServicesRepository;

	@Autowired
	private OutboxService outboxService;

	@Autowired
	private AppointmentProjectionRepository appointmentProjectionRepository;

	private static final Logger logger = LoggerFactory.getLogger(AvailabilitySlotService.class);

	/**
	 * Regenerates availability day by day (manual generation and rule-change
	 * propagation). Days that contain an active (SCHEDULED/CONFIRMED/
	 * RESCHEDULED) appointment are skipped — their existing slots are left
	 * untouched — so one booking never blocks regeneration of the whole range.
	 * Each regenerated day publishes its own AvailabilitySlotsGenerated event;
	 * the AppointmentService projection refuses to touch booked slots, and
	 * skipping booked days here keeps both sides consistent.
	 */
	@Transactional
	public List<TherapistAvailability> generateAvailabilitySlots(String therapistId, LocalDate startDate, LocalDate endDate) throws JsonProcessingException {
		logger.info("inside generateTherapistAvailabilitySlots");

		List<TherapistAvailability> allSlotsInRange = new ArrayList<>();
		List<LocalDate> skippedDays = new ArrayList<>();

		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

			LocalDateTime dayStart = date.atStartOfDay();
			LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

			if (hasActiveAppointmentsOverlap(therapistId, dayStart, dayEnd)) {
				skippedDays.add(date);
				allSlotsInRange.addAll(therapistAvailabilityRepository
						.findByTherapistIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(therapistId, dayStart, dayEnd));
				continue;
			}

			// destructive sync: an empty day still publishes so the
			// AppointmentService projection clears removed slots
			allSlotsInRange.addAll(regenerateDay(therapistId, date, true));
		}

		if (!skippedDays.isEmpty()) {
			logger.info("Slot generation for therapistId={} skipped days with active appointments: {}", therapistId, skippedDays);
		}

		return allSlotsInRange;
	}

	/**
	 * Nightly horizon extension: generates ONLY days that have no slots yet
	 * (normally just the day newly entering the window). Days that already
	 * have slots are left exactly as they are — rule changes propagate
	 * immediately at edit time via generateAvailabilitySlots, so re-chopping
	 * unchanged days every night would only churn slotIds and events.
	 */
	@Transactional
	public void generateMissingDays(String therapistId, LocalDate startDate, LocalDate endDate) throws JsonProcessingException {

		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

			LocalDateTime dayStart = date.atStartOfDay();
			LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

			boolean dayHasSlots = !therapistAvailabilityRepository
					.findByTherapistIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(therapistId, dayStart, dayEnd)
					.isEmpty();

			if (dayHasSlots || hasActiveAppointmentsOverlap(therapistId, dayStart, dayEnd)) {
				continue;
			}

			// no event when the day yields nothing (e.g. no rules for that
			// weekday) — there is nothing to sync and nothing to clear
			regenerateDay(therapistId, date, false);
		}
	}

	private List<TherapistAvailability> regenerateDay(String therapistId, LocalDate date, boolean publishWhenEmpty) throws JsonProcessingException {

		LocalDateTime dayStart = date.atStartOfDay();
		LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

		therapistAvailabilityRepository.deleteInRange(therapistId, dayStart, dayEnd);

		List<TherapistAvailability> baseSlots = generateBaseSlotsFromRules(therapistId, date, date);
		if (!baseSlots.isEmpty()) {
			therapistAvailabilityRepository.saveAll(baseSlots);
		}

		applyAvailableOverridesForRange(therapistId, date, date);

		List<TherapistAvailability> daySlots = therapistAvailabilityRepository
				.findByTherapistIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(therapistId, dayStart, dayEnd);

		if (!daySlots.isEmpty() || publishWhenEmpty) {
			AvailabilitySlotsGeneratedEvent availabilitySlotsGeneratedEvent = buildSlotsGeneratedEvent(therapistId, date, date, daySlots);
			outboxService.saveOutboxEvent("THERAPIST_AVAILABILITY", therapistId, "AvailabilitySlotsGenerated", availabilitySlotsGeneratedEvent);
		}

		return daySlots;
	}

	private List<TherapistAvailability> generateBaseSlotsFromRules(String therapistId, LocalDate startDate, LocalDate endDate) {
		List<TherapistAvailability> newSlotsToSave = new ArrayList<>();
		List<TherapistServices> therapistServices = therapistServicesRepository.findByTherapistIdAndIsActiveTrue(therapistId);

		if (therapistServices.isEmpty()) {
			return new ArrayList<>();
		}

		List<TherapistAvailabilityRules> therapistAvailabilityRulesList = therapistAvailabilityRulesRepository.findByTherapistIdAndIsActiveTrue(therapistId);

		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			int dayOfWeek = date.getDayOfWeek().getValue();
			List<TherapistAvailabilityRules> applicableRules = therapistAvailabilityRulesList.stream()
					.filter(rule -> rule.getDayOfWeek() == dayOfWeek)
					.toList();

			for (TherapistAvailabilityRules rule : applicableRules) {
				newSlotsToSave.addAll(
						chopTimeBlockIntoSlots(
								therapistId,
								date,
								rule.getStartTime(),
								rule.getEndTime(),
								therapistServices));
			}
		}
		return newSlotsToSave;
	}

	@Transactional
	public void applyAvailableOverridesForRange(String therapistId, LocalDate startDate, LocalDate endDate) throws JsonProcessingException {
		List<TherapistAvailabilityOverrides> overrides = therapistAvailabilityOverridesRepository.findByTherapistIdAndStartTimeBetweenOrderByStartTimeAsc(
				therapistId,
				startDate.atStartOfDay(),
				endDate.plusDays(1).atStartOfDay());

		for (TherapistAvailabilityOverrides override : overrides) {
			if (!override.isAvailable()) {
				continue;
			}
			addAvailableOverrideSlots(therapistId, override.getStartTime(), override.getEndTime());
		}
	}

	@Transactional
	public void applyCreatedOverride(String therapistId, TherapistAvailabilityOverrides override) throws JsonProcessingException {
		validateSingleDayWindow(override.getStartTime(), override.getEndTime());
		rejectIfActiveAppointmentsOverlap(therapistId, override.getStartTime(), override.getEndTime());

		if (!override.isAvailable()) {
			return;
		}

		List<TherapistAvailability> createdSlots = addAvailableOverrideSlots(therapistId, override.getStartTime(), override.getEndTime());
		publishSlotCreatedEvents(createdSlots);
	}

	@Transactional
	public void applyDeletedOverride(String therapistId, TherapistAvailabilityOverrides override) throws JsonProcessingException {
		validateSingleDayWindow(override.getStartTime(), override.getEndTime());
		rejectIfActiveAppointmentsOverlap(therapistId, override.getStartTime(), override.getEndTime());

		if (!override.isAvailable()) {
			return;
		}

		List<TherapistAvailability> removedSlots = therapistAvailabilityRepository.findByTherapistIdAndStartTimeLessThanAndEndTimeGreaterThan(
				therapistId,
				override.getEndTime(),
				override.getStartTime());

		therapistAvailabilityRepository.deleteOverlappingSlots(therapistId, override.getStartTime(), override.getEndTime());
		publishSlotRemovedEvents(removedSlots);
	}

	private List<TherapistAvailability> addAvailableOverrideSlots(String therapistId, LocalDateTime windowStart, LocalDateTime windowEnd) {
		validateSingleDayWindow(windowStart, windowEnd);

		LocalDate date = windowStart.toLocalDate();
		List<TherapistServices> therapistServices = therapistServicesRepository.findByTherapistIdAndIsActiveTrue(therapistId);
		List<TherapistAvailability> slotsToSave = new ArrayList<>();

		List<TherapistAvailability> existing = therapistAvailabilityRepository
				.findByTherapistIdAndStartTimeLessThanAndEndTimeGreaterThan(therapistId, windowEnd, windowStart);
		Set<String> existingKeys = new HashSet<String>();
		for (TherapistAvailability s : existing) {
			existingKeys.add(s.getServiceId() + "|" + s.getStartTime() + "|" + s.getEndTime());
		}

		for (TherapistAvailability slot : chopTimeBlockIntoSlots(
				therapistId,
				date,
				windowStart.toLocalTime(),
				windowEnd.toLocalTime(),
				therapistServices)) {
			String key = slot.getServiceId() + "|" + slot.getStartTime() + "|" + slot.getEndTime();
			if (!existingKeys.contains(key)) {
				slotsToSave.add(slot);
			}
		}

		if (slotsToSave.isEmpty()) {
			return new ArrayList<>();
		}

		return therapistAvailabilityRepository.saveAll(slotsToSave);
	}

	private void publishSlotCreatedEvents(List<TherapistAvailability> createdSlots) throws JsonProcessingException {
		for (TherapistAvailability slot : createdSlots) {
			AvailabilityEvent event = new AvailabilityEvent();
			event.setEventType("AvailabilitySlotCreated");
			event.setSlotId(slot.getSlotId());
			event.setTherapistId(slot.getTherapistId());
			event.setServiceId(slot.getServiceId());
			event.setSessionFee(slot.getSessionFee());
			event.setStartTime(slot.getStartTime());
			event.setEndTime(slot.getEndTime());

			outboxService.saveOutboxEvent("THERAPIST_AVAILABILITY", slot.getTherapistId(), "AvailabilitySlotCreated", event);
		}
	}

	private void publishSlotRemovedEvents(List<TherapistAvailability> removedSlots) throws JsonProcessingException {
		for (TherapistAvailability slot : removedSlots) {
			AvailabilityEvent event = new AvailabilityEvent();
			event.setEventType("AvailabilitySlotRemoved");
			event.setSlotId(slot.getSlotId());
			event.setTherapistId(slot.getTherapistId());
			event.setSessionFee(slot.getSessionFee());
			event.setStartTime(slot.getStartTime());
			event.setEndTime(slot.getEndTime());

			outboxService.saveOutboxEvent("THERAPIST_AVAILABILITY", slot.getTherapistId(), "AvailabilitySlotRemoved", event);
		}
	}

	private boolean hasActiveAppointmentsOverlap(String therapistId, LocalDateTime windowStart, LocalDateTime windowEnd) {
		return appointmentProjectionRepository.existsByTherapistIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
				therapistId,
				List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED, AppointmentStatus.RESCHEDULED),
				windowEnd,
				windowStart);
	}

	private void rejectIfActiveAppointmentsOverlap(String therapistId, LocalDateTime windowStart, LocalDateTime windowEnd) {
		if (hasActiveAppointmentsOverlap(therapistId, windowStart, windowEnd)) {
			throw new IllegalStateException("Override window overlaps active appointments.");
		}
	}

	private void validateSingleDayWindow(LocalDateTime start, LocalDateTime end) {
		if (!start.toLocalDate().equals(end.toLocalDate())) {
			throw new IllegalArgumentException("Override application currently supports same-day windows only.");
		}
	}

	private List<TherapistAvailability> chopTimeBlockIntoSlots(String therapistId, LocalDate date, LocalTime startTime, LocalTime endTime, List<TherapistServices> therapistServices) {
		List<TherapistAvailability> newSlots = new ArrayList<>();

		for (TherapistServices service : therapistServices) {
			int slotDurationMinutes = service.getDuration();
			String serviceId = service.getServiceId();
			LocalTime currentSlotTime = startTime;

			while (currentSlotTime.isBefore(endTime)) {
				LocalDateTime slotStartTime = LocalDateTime.of(date, currentSlotTime);
				LocalDateTime slotEndTime = slotStartTime.plusMinutes(slotDurationMinutes);

				if (slotEndTime.isAfter(LocalDateTime.of(date, endTime))) {
					break;
				}

				TherapistAvailability slot = new TherapistAvailability();
				slot.setTherapistId(therapistId);
				slot.setStartTime(slotStartTime);
				slot.setEndTime(slotEndTime);
				slot.setServiceId(serviceId);
				// no fee on slots: pricing lives on delivery modes and is
				// resolved at booking time; slot sessionFee was written but
				// never read anywhere downstream

				newSlots.add(slot);
				currentSlotTime = currentSlotTime.plusMinutes(30);
			}
		}
		return newSlots;
	}

	private AvailabilitySlotsGeneratedEvent buildSlotsGeneratedEvent(String therapistId, LocalDate startDate, LocalDate endDate, List<TherapistAvailability> newSlots) {
		AvailabilitySlotsGeneratedEvent event = new AvailabilitySlotsGeneratedEvent();
		event.setTherapistId(therapistId);
		event.setRangeStart(startDate);
		event.setRangeEnd(endDate);

		List<Slot> slotList = new ArrayList<>();
		for (TherapistAvailability a : newSlots) {
			Slot p = new Slot();
			p.setSlotId(a.getSlotId());
			p.setServiceId(a.getServiceId());
			p.setSessionFee(a.getSessionFee());
			p.setStartTime(a.getStartTime());
			p.setEndTime(a.getEndTime());
			slotList.add(p);
		}

		event.setSlotList(slotList);
		return event;
	}

	@Transactional
	public boolean deleteAvailabilitySlots(String therapistId, LocalDate startDate, LocalDate endDate) throws JsonProcessingException {
		rejectIfActiveAppointmentsOverlap(therapistId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
		therapistAvailabilityRepository.deleteInRange(therapistId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

		AvailabilitySlotsDeletedEvent availabilitySlotsDeletedEvent = new AvailabilitySlotsDeletedEvent();
		availabilitySlotsDeletedEvent.setOccurredAt(LocalDateTime.now());
		availabilitySlotsDeletedEvent.setTherapistId(therapistId);
		availabilitySlotsDeletedEvent.setRangeStart(startDate);
		availabilitySlotsDeletedEvent.setRangeEnd(endDate);

		outboxService.saveOutboxEvent("THERAPIST_AVAILABILITY", therapistId, "AvailabilitySlotsDeleted", availabilitySlotsDeletedEvent);
		return true;
	}

}
