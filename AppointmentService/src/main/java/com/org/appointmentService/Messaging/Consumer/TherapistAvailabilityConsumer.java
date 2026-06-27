package com.org.appointmentService.Messaging.Consumer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.appointmentService.Entity.TherapistAvailability;
import com.org.appointmentService.Enums.AvailabilityStatus;
import com.org.appointmentService.Exception.AvailabilityProjectionConflictException;
import com.org.appointmentService.Repository.TherapistAvailabilityRepository;
import com.org.events.TherapistAvailability.AvailabilityEvent;
import com.org.events.TherapistAvailability.AvailabilitySlotsDeletedEvent;
import com.org.events.TherapistAvailability.AvailabilitySlotsGeneratedEvent;
import com.org.events.TherapistAvailability.Slot;

import jakarta.transaction.Transactional;

@Component
public class TherapistAvailabilityConsumer {

	@Autowired
	TherapistAvailabilityRepository therapistAvailabilityRepository;

	@Autowired
	ObjectMapper objectMapper;

	private static final String topic = "therapist-availability-events";

	private static final Logger logger = LoggerFactory.getLogger(TherapistAvailabilityConsumer.class);

	@KafkaListener(topics = topic, groupId = "appointment-availability-slot-projection-group")
	@Transactional
	public void process(JsonNode payload) {
		logger.debug("inside process of therapist-availability-events..");

		String eventType = payload.get("eventType").asText();

		switch (eventType) {

		case "AvailabilitySlotsGenerated" -> {
			AvailabilitySlotsGeneratedEvent batchEvent = objectMapper.convertValue(payload, AvailabilitySlotsGeneratedEvent.class);
			processBatchGeneration(batchEvent);
		}

		case "AvailabilitySlotsDeleted" -> {
			AvailabilitySlotsDeletedEvent batchEvent = objectMapper.convertValue(payload, AvailabilitySlotsDeletedEvent.class);
			processBatchDeletion(batchEvent);
		}

		case "AvailabilitySlotCreated",
		"AvailabilitySlotRemoved" -> {
			AvailabilityEvent singleEvent = objectMapper.convertValue(payload, AvailabilityEvent.class);
			processSingleSlotEvent(singleEvent);
		}
		}
	}

	private void processSingleSlotEvent(AvailabilityEvent singleEvent) {

		if ("AvailabilitySlotCreated".equals(singleEvent.getEventType())) {
			processSlotCreated(singleEvent);
			return;
		}

		if ("AvailabilitySlotRemoved".equals(singleEvent.getEventType())) {
			processSlotRemoved(singleEvent);
			return;
		}
	}

	private void processSlotCreated(AvailabilityEvent event) {

		if (therapistAvailabilityRepository.existsBySlotId(event.getSlotId())) {
			return;
		}

		TherapistAvailability therapistAvailability = new TherapistAvailability();
		therapistAvailability.setSlotId(event.getSlotId());
		therapistAvailability.setTherapistId(event.getTherapistId());
		therapistAvailability.setSessionFee(event.getSessionFee());
		therapistAvailability.setServiceId(event.getServiceId());
		therapistAvailability.setStartTime(event.getStartTime());
		therapistAvailability.setEndTime(event.getEndTime());
		therapistAvailability.setStatus(AvailabilityStatus.AVAILABLE);

		therapistAvailabilityRepository.save(therapistAvailability);
	}

	private void processSlotRemoved(AvailabilityEvent event) {

		TherapistAvailability therapistAvailability = therapistAvailabilityRepository.findBySlotIdAndTherapistId(event.getSlotId(), event.getTherapistId())
				.orElse(null);

		if (therapistAvailability == null) {
			return;
		}

		if (AvailabilityStatus.BOOKED.equals(therapistAvailability.getStatus())) {
			return;
		}

		therapistAvailabilityRepository.delete(therapistAvailability);
	}

	private void processBatchGeneration(AvailabilitySlotsGeneratedEvent event) {

		logger.debug("inside processBatchGeneration..");

		LocalDateTime start = event.getRangeStart().atStartOfDay();
		LocalDateTime end = event.getRangeEnd().plusDays(1).atStartOfDay();

		rejectIfBookedSlotsExist(event.getTherapistId(), start, end, "AvailabilitySlotsGenerated");

		therapistAvailabilityRepository.deleteAvailableSlotsInRange(event.getTherapistId(), start, end);

		List<TherapistAvailability> therapistAvailabilityList = new ArrayList<>();

		for (Slot slot : event.getSlotList()) {

			if (therapistAvailabilityRepository.existsBySlotId(slot.getSlotId())) {
				continue;
			}

			TherapistAvailability therapistAvailability = new TherapistAvailability();
			therapistAvailability.setSlotId(slot.getSlotId());
			therapistAvailability.setTherapistId(event.getTherapistId());
			therapistAvailability.setSessionFee(slot.getSessionFee());
			therapistAvailability.setServiceId(slot.getServiceId());
			therapistAvailability.setStartTime(slot.getStartTime());
			therapistAvailability.setEndTime(slot.getEndTime());
			therapistAvailability.setStatus(AvailabilityStatus.AVAILABLE);

			therapistAvailabilityList.add(therapistAvailability);
		}

		if (!therapistAvailabilityList.isEmpty()) {
			therapistAvailabilityRepository.saveAll(therapistAvailabilityList);
		}

		logger.debug("exiting processBatchGeneration..");
	}

	public void processBatchDeletion(AvailabilitySlotsDeletedEvent event) {

		logger.debug("inside processBatchDeletion..");

		LocalDateTime start = event.getRangeStart().atStartOfDay();
		LocalDateTime end = event.getRangeEnd().plusDays(1).atStartOfDay();

		rejectIfBookedSlotsExist(event.getTherapistId(), start, end, "AvailabilitySlotsDeleted");

		therapistAvailabilityRepository.deleteAvailableSlotsInRange(event.getTherapistId(), start, end);
	}

	private void rejectIfBookedSlotsExist(String therapistId, LocalDateTime start, LocalDateTime end, String eventType) {
		boolean hasBookedSlots = therapistAvailabilityRepository.existsByTherapistIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
				therapistId,
				AvailabilityStatus.BOOKED,
				end,
				start);

		if (!hasBookedSlots) {
			return;
		}

		List<TherapistAvailability> conflictingSlots = therapistAvailabilityRepository
				.findTop10ByTherapistIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
						therapistId,
						AvailabilityStatus.BOOKED,
						end,
						start);

		logger.error(
				"Rejecting {} for therapistId={} because booked slots exist in range {} to {}. Conflicts={}",
				eventType,
				therapistId,
				start,
				end,
				conflictingSlots.stream().map(TherapistAvailability::getSlotId).toList());

		throw new AvailabilityProjectionConflictException(
				"Booked slots exist in range. Refusing to apply destructive availability sync for therapistId="
						+ therapistId + ", eventType=" + eventType);
	}
}
