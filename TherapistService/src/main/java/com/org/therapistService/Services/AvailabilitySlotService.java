package com.org.therapistService.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import com.org.therapistService.Enums.SessionType;
import com.org.therapistService.Repository.AppointmentProjectionRepository;
import com.org.therapistService.Repository.TherapistAvailabilityOverridesRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;
import com.org.therapistService.Repository.TherapistServicesRepository;

import jakarta.transaction.Transactional;

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

	@Transactional
	public List<TherapistAvailability> generateAvailabilitySlots(String therapistId, LocalDate startDate, LocalDate endDate) throws JsonProcessingException {
		logger.info("inside generateTherapistAvailabilitySlots");

		rejectIfActiveAppointmentsOverlap(therapistId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
		therapistAvailabilityRepository.deleteInRange(therapistId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

		List<TherapistAvailability> baseSlots = generateBaseSlotsFromRules(therapistId, startDate, endDate);
		if (!baseSlots.isEmpty()) {
			therapistAvailabilityRepository.saveAll(baseSlots);
		}

		applyOverridesForRange(therapistId, startDate, endDate);

		List<TherapistAvailability> finalSlots = therapistAvailabilityRepository.findByTherapistIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
				therapistId,
				startDate.atStartOfDay(),
				endDate.plusDays(1).atStartOfDay());

		if (!finalSlots.isEmpty()) {
			AvailabilitySlotsGeneratedEvent availabilitySlotsGeneratedEvent = buildSlotsGeneratedEvent(therapistId, startDate, endDate, finalSlots);
			outboxService.saveOutboxEvent("THERAPIST_AVAILABILITY", therapistId, "AvailabilitySlotsGenerated", availabilitySlotsGeneratedEvent);
			return finalSlots;
		}

		return new ArrayList<>();
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
								null,
								therapistServices));
			}
		}
		return newSlotsToSave;
	}

	@Transactional
	public void applyOverridesForRange(String therapistId, LocalDate startDate, LocalDate endDate) throws JsonProcessingException {
		List<TherapistAvailabilityOverrides> overrides = therapistAvailabilityOverridesRepository.findByTherapistIdAndStartTimeBetweenOrderByStartTimeAsc(
				therapistId,
				startDate.atStartOfDay(),
				endDate.plusDays(1).atStartOfDay());

		for (TherapistAvailabilityOverrides override : overrides) {
			applySingleOverride(therapistId, override, false);
		}
	}

	@Transactional
	public void applyCreatedOverride(String therapistId, TherapistAvailabilityOverrides override) throws JsonProcessingException {
		applySingleOverride(therapistId, override, true);
	}

	@Transactional
	public void applyDeletedOverride(String therapistId, TherapistAvailabilityOverrides deletedOverride) throws JsonProcessingException {
		validateSingleDayWindow(deletedOverride.getStartTime(), deletedOverride.getEndTime());
		rebuildWindow(therapistId, deletedOverride.getStartTime(), deletedOverride.getEndTime(), true);
	}

	private void applySingleOverride(String therapistId, TherapistAvailabilityOverrides override, boolean publishEvents) throws JsonProcessingException {
		validateSingleDayWindow(override.getStartTime(), override.getEndTime());
		rejectIfActiveAppointmentsOverlap(therapistId, override.getStartTime(), override.getEndTime());

		if (!override.isAvailable()) {
			List<TherapistAvailability> removedSlots = therapistAvailabilityRepository
					.findByTherapistIdAndStartTimeLessThanAndEndTimeGreaterThan(
							therapistId,
							override.getEndTime(),
							override.getStartTime());

			therapistAvailabilityRepository.deleteOverlappingSlots(therapistId, override.getStartTime(), override.getEndTime());
			if (publishEvents) {
				publishSlotRemovedEvents(removedSlots);
			}
			return;
		}

		List<TherapistAvailability> createdSlots = addAvailableOverrideSlots(therapistId, override.getStartTime(), override.getEndTime());
		if (publishEvents) {
			publishSlotCreatedEvents(createdSlots);
		}
	}

	private void rebuildWindow(String therapistId, LocalDateTime windowStart, LocalDateTime windowEnd, boolean publishEvents) throws JsonProcessingException {
		rejectIfActiveAppointmentsOverlap(therapistId, windowStart, windowEnd);

		List<TherapistAvailability> beforeSlots = therapistAvailabilityRepository
				.findByTherapistIdAndStartTimeLessThanAndEndTimeGreaterThan(
						therapistId,
						windowEnd,
						windowStart);

		therapistAvailabilityRepository.deleteOverlappingSlots(therapistId, windowStart, windowEnd);
		rebuildBaseSlotsForWindow(therapistId, windowStart, windowEnd);
		reapplyRemainingOverridesForWindow(therapistId, windowStart, windowEnd);

		if (!publishEvents) {
			return;
		}

		List<TherapistAvailability> afterSlots = therapistAvailabilityRepository
				.findByTherapistIdAndStartTimeLessThanAndEndTimeGreaterThan(
						therapistId,
						windowEnd,
						windowStart);

		Map<String, TherapistAvailability> beforeBySlotId = beforeSlots.stream()
				.collect(Collectors.toMap(TherapistAvailability::getSlotId, Function.identity()));
		Map<String, TherapistAvailability> afterBySlotId = afterSlots.stream()
				.collect(Collectors.toMap(TherapistAvailability::getSlotId, Function.identity()));

		List<TherapistAvailability> removedSlots = beforeSlots.stream()
				.filter(slot -> !afterBySlotId.containsKey(slot.getSlotId()))
				.toList();
		List<TherapistAvailability> createdSlots = afterSlots.stream()
				.filter(slot -> !beforeBySlotId.containsKey(slot.getSlotId()))
				.toList();

		publishSlotRemovedEvents(removedSlots);
		publishSlotCreatedEvents(createdSlots);
	}

	private void rebuildBaseSlotsForWindow(String therapistId, LocalDateTime windowStart, LocalDateTime windowEnd) {
		validateSingleDayWindow(windowStart, windowEnd);

		LocalDate date = windowStart.toLocalDate();
		int dayOfWeek = date.getDayOfWeek().getValue();
		List<TherapistServices> therapistServices = therapistServicesRepository.findByTherapistIdAndIsActiveTrue(therapistId);
		List<TherapistAvailabilityRules> applicableRules = therapistAvailabilityRulesRepository.findByTherapistIdAndIsActiveTrue(therapistId)
				.stream()
				.filter(rule -> rule.getDayOfWeek() == dayOfWeek)
				.toList();

		List<TherapistAvailability> slotsToSave = new ArrayList<>();
		for (TherapistAvailabilityRules rule : applicableRules) {
			TimeBlock intersection = intersect(
					new TimeBlock(rule.getStartTime(), rule.getEndTime()),
					windowStart.toLocalTime(),
					windowEnd.toLocalTime());
			if (intersection == null) {
				continue;
			}

			for (TherapistAvailability slot : chopTimeBlockIntoSlots(therapistId, date, intersection.start(), intersection.end(), null, therapistServices)) {
				if (!therapistAvailabilityRepository.existsByTherapistIdAndServiceIdAndStartTimeAndEndTime(
						therapistId,
						slot.getServiceId(),
						slot.getStartTime(),
						slot.getEndTime())) {
					slotsToSave.add(slot);
				}
			}
		}

		if (!slotsToSave.isEmpty()) {
			therapistAvailabilityRepository.saveAll(slotsToSave);
		}
	}

	private void reapplyRemainingOverridesForWindow(String therapistId, LocalDateTime windowStart, LocalDateTime windowEnd) {
		LocalDate date = windowStart.toLocalDate();
		List<TherapistAvailabilityOverrides> overrides = therapistAvailabilityOverridesRepository.findByTherapistIdAndStartTimeBetweenOrderByStartTimeAsc(
				therapistId,
				date.atStartOfDay(),
				date.plusDays(1).atStartOfDay());

		for (TherapistAvailabilityOverrides override : overrides) {
			TimeBlock overlap = intersect(
					new TimeBlock(override.getStartTime().toLocalTime(), override.getEndTime().toLocalTime()),
					windowStart.toLocalTime(),
					windowEnd.toLocalTime());
			if (overlap == null) {
				continue;
			}

			TherapistAvailabilityOverrides clipped = new TherapistAvailabilityOverrides();
			clipped.setTherapistId(override.getTherapistId());
			clipped.setAvailable(override.isAvailable());
			clipped.setStartTime(LocalDateTime.of(date, overlap.start()));
			clipped.setEndTime(LocalDateTime.of(date, overlap.end()));
			applySingleOverrideSilently(therapistId, clipped);
		}
	}

	private void applySingleOverrideSilently(String therapistId, TherapistAvailabilityOverrides override) {
		try {
			applySingleOverride(therapistId, override, false);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to apply override silently.", e);
		}
	}

	private List<TherapistAvailability> addAvailableOverrideSlots(String therapistId, LocalDateTime windowStart, LocalDateTime windowEnd) {
		validateSingleDayWindow(windowStart, windowEnd);

		LocalDate date = windowStart.toLocalDate();
		List<TherapistServices> therapistServices = therapistServicesRepository.findByTherapistIdAndIsActiveTrue(therapistId);
		List<TherapistAvailability> slotsToSave = new ArrayList<>();

		for (TherapistAvailability slot : chopTimeBlockIntoSlots(
				therapistId,
				date,
				windowStart.toLocalTime(),
				windowEnd.toLocalTime(),
				null,
				therapistServices)) {
			if (!therapistAvailabilityRepository.existsByTherapistIdAndServiceIdAndStartTimeAndEndTime(
					therapistId,
					slot.getServiceId(),
					slot.getStartTime(),
					slot.getEndTime())) {
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
			event.setStartTime(slot.getStartTime());
			event.setEndTime(slot.getEndTime());

			outboxService.saveOutboxEvent("THERAPIST_AVAILABILITY", slot.getTherapistId(), "AvailabilitySlotRemoved", event);
		}
	}

	private void rejectIfActiveAppointmentsOverlap(String therapistId, LocalDateTime windowStart, LocalDateTime windowEnd) {
		boolean hasOverlap = appointmentProjectionRepository.existsByTherapistIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
				therapistId,
				List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED, AppointmentStatus.RESCHEDULED),
				windowEnd,
				windowStart);

		if (hasOverlap) {
			throw new IllegalStateException("Override window overlaps active appointments.");
		}
	}

	private void validateSingleDayWindow(LocalDateTime start, LocalDateTime end) {
		if (!start.toLocalDate().equals(end.toLocalDate())) {
			throw new IllegalArgumentException("Override application currently supports same-day windows only.");
		}
	}

	private List<TherapistAvailability> chopTimeBlockIntoSlots(String therapistId, LocalDate date, LocalTime startTime, LocalTime endTime, SessionType sessionType, List<TherapistServices> therapistServices) {
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
				slot.setSessionType(sessionType);

				newSlots.add(slot);
				currentSlotTime = currentSlotTime.plusMinutes(30);
			}
		}
		return newSlots;
	}

	private TimeBlock intersect(TimeBlock base, LocalTime windowStart, LocalTime windowEnd) {
		LocalTime start = max(base.start(), windowStart);
		LocalTime end = min(base.end(), windowEnd);
		return start.isBefore(end) ? new TimeBlock(start, end) : null;
	}

	private LocalTime min(LocalTime first, LocalTime second) {
		return first.isBefore(second) ? first : second;
	}

	private LocalTime max(LocalTime first, LocalTime second) {
		return first.isAfter(second) ? first : second;
	}

	private AvailabilitySlotsGeneratedEvent buildSlotsGeneratedEvent(String therapistId, LocalDate startDate, LocalDate endDate, List<TherapistAvailability> newSlots) {
		AvailabilitySlotsGeneratedEvent event = new AvailabilitySlotsGeneratedEvent();
		event.setTherapistId(therapistId);
		event.setRangeStart(startDate);
		event.setRangeEnd(endDate);

		List<Slot> slotList = newSlots.stream()
				.map(a -> {
					Slot p = new Slot();
					p.setSlotId(a.getSlotId());
					p.setStartTime(a.getStartTime());
					p.setEndTime(a.getEndTime());
					return p;
				})
				.toList();

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

	private record TimeBlock(LocalTime start, LocalTime end) {
	}
}
