package com.org.therapistService.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.events.TherapistAvailability.AvailabilitySlotsGeneratedEvent;
import com.org.events.TherapistAvailability.Slot;
import com.org.therapistService.Entity.OutboxEvent;
import com.org.therapistService.Entity.TherapistAvailability;
import com.org.therapistService.Entity.TherapistAvailabilityOverrides;
import com.org.therapistService.Entity.TherapistAvailabilityRules;
import com.org.therapistService.Entity.TherapistServices;
import com.org.therapistService.Enums.SessionType;
import com.org.therapistService.Repository.OutboxEventRepository;
import com.org.therapistService.Repository.TherapistAvailabilityOverridesRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;
import com.org.therapistService.Repository.TherapistServicesRepository;

import jakarta.transaction.Transactional;

@Service
public class AvailabilitySlotGeneratorService {

	@Autowired
	private TherapistAvailabilityRulesRepository therapistAvailabilityRulesRepository;

	@Autowired
	private TherapistAvailabilityOverridesRepository therapistAvailabilityOverridesRepository;

	@Autowired
	private TherapistAvailabilityRepository therapistAvailabilityRepository;

	@Autowired
	private TherapistServicesRepository therapistServicesRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	private static final Logger logger = LoggerFactory.getLogger(AvailabilitySlotGeneratorService.class);

	@Transactional
	public List<TherapistAvailability> generateTherapistAvailabilitySlots(String therapistId, LocalDate startDate, LocalDate endDate) {
		logger.info("inside generateTherapistAvailabilitySlots");

		therapistAvailabilityRepository.deleteInRange(therapistId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

		List<TherapistAvailability> newSlotsToSave = new ArrayList<>();

		List<TherapistServices> therapistServices = therapistServicesRepository.findByTherapistIdAndIsActiveTrue(therapistId);

		// If the therapist has no active services, we can't generate anything
		if (therapistServices.isEmpty()) {
			return new ArrayList<>(); 
		}

		List<TherapistAvailabilityOverrides> overrides = therapistAvailabilityOverridesRepository.findByTherapistIdAndStartTimeBetween(therapistId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
		List<TherapistAvailabilityRules> therapistAvailabilityRulesList = therapistAvailabilityRulesRepository.findByTherapistIdAndIsActiveTrue(therapistId);

		for(LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

			LocalDate finalDate = date;
			boolean isUnavailable = overrides.stream().anyMatch(o ->
			!o.isAvailable() && o.getStartTime().toLocalDate().equals(finalDate));

			if(isUnavailable) {
				continue;
			}
			int dayOfWeek = date.getDayOfWeek().getValue();
			//List<TherapistAvailabilityRules> rulesForDay = therapistAvailabilityRulesRepository.findByTherapistIdAndDayOfWeekAndIsActiveTrue(therapistId, dayOfWeek);
			List<TherapistAvailabilityRules> applicableRules = therapistAvailabilityRulesList.stream()
					.filter(rule -> rule.getDayOfWeek() == dayOfWeek)
					.toList();

			for (TherapistAvailabilityRules rule : applicableRules) {
				newSlotsToSave.addAll(
						chopTimeBlockIntoSlots(therapistId, finalDate, rule.getStartTime(), rule.getEndTime(), rule.getSessionType(), therapistServices)
						);
			}

			// 7. Find and process "AVAILABLE" overrides for this day
			// Usage: We use 'finalDate' here as well
			overrides.stream()
			.filter(o -> o.isAvailable() && o.getStartTime().toLocalDate().equals(finalDate))
			.forEach(override -> newSlotsToSave.addAll(
					chopTimeBlockIntoSlots(
							therapistId, 
							finalDate, 
							override.getStartTime().toLocalTime(), 
							override.getEndTime().toLocalTime(),
							override.getSessionType(),
							therapistServices
							)
					));
		}
		// 8. Save all new slots in a single batch
		if (!newSlotsToSave.isEmpty()) {
			logger.info("exiting generateTherapistAvailabilitySlots");
			therapistAvailabilityRepository.saveAll(newSlotsToSave);
			AvailabilitySlotsGeneratedEvent event = buildEvent(therapistId, startDate, endDate, newSlotsToSave);
			persistOutboxEvent(event, therapistId);
			return newSlotsToSave;
		}

		logger.info("exiting generateTherapistAvailabilitySlots");
		return new ArrayList<>();

	}

	private List<TherapistAvailability> chopTimeBlockIntoSlots(String therapistId, LocalDate date, LocalTime startTime, LocalTime endTime, SessionType sessionType, List<TherapistServices> therapistServices) {

		List<TherapistAvailability> newSlots = new ArrayList<>();

		// Iterate through ALL of the therapist's available services
		for (TherapistServices service : therapistServices) {

			// The duration for this specific service
			int slotDurationMinutes = service.getDuration();
			String serviceId = service.getServiceId();

			LocalTime currentSlotTime = startTime;

			// Start chopping the block using the service's duration
			while (currentSlotTime.isBefore(endTime)) {

				LocalDateTime slotStartTime = LocalDateTime.of(date, currentSlotTime);
				LocalDateTime slotEndTime = slotStartTime.plusMinutes(slotDurationMinutes);

				// Stop if the slot extends past the end of the time block
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


				// Move to the next potential start time
				currentSlotTime = currentSlotTime.plusMinutes(slotDurationMinutes);
			}
		}
		return newSlots;
	}

	private AvailabilitySlotsGeneratedEvent buildEvent(String therapistId, LocalDate startDate, LocalDate endDate, List<TherapistAvailability> newSlots) {

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

	private void persistOutboxEvent(AvailabilitySlotsGeneratedEvent event, String therapistId) {
		try {
			OutboxEvent outbox = new OutboxEvent();
			outbox.setAggregateType("THERAPIST_AVAILABILITY");
			outbox.setAggregateId(therapistId);
			outbox.setEventType("AvailabilitySlotsGenerated");
			outbox.setPayload(objectMapper.writeValueAsString(event));
			outbox.setCreatedAt(LocalDateTime.now());
			outbox.setPublished(false);

			outboxEventRepository.save(outbox);

		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize event", e);
		}
	}
}

