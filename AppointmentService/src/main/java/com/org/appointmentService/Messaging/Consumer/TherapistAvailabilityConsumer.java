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

		// never offer a slot on top of a booked time, even if the producer
		// believed the window was free (projection lag)
		boolean overlapsBooked = therapistAvailabilityRepository
				.existsByTherapistIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
						event.getTherapistId(),
						AvailabilityStatus.BOOKED,
						event.getEndTime(),
						event.getStartTime());

		if (overlapsBooked) {
			logger.warn("Skipping AvailabilitySlotCreated slotId={} — overlaps a BOOKED slot. therapistId={}",
					event.getSlotId(), event.getTherapistId());
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

	/**
	 * Applies a (re)generation snapshot for the event's range. The producer
	 * skips days with active appointments, so BOOKED slots are not expected
	 * here — but if one exists anyway (ABANDONED appointment keeping its slot,
	 * or projection lag on a fresh booking), the sync converges instead of
	 * rejecting: BOOKED rows are never deleted, and incoming slots that
	 * overlap a BOOKED time are skipped so an occupied time is never offered
	 * twice. Rejecting here would only park the event in the DLT and leave
	 * this projection permanently stale for that day.
	 */
	private void processBatchGeneration(AvailabilitySlotsGeneratedEvent event) {

		logger.debug("inside processBatchGeneration..");

		LocalDateTime start = event.getRangeStart().atStartOfDay();
		LocalDateTime end = event.getRangeEnd().plusDays(1).atStartOfDay();

		List<TherapistAvailability> bookedSlots = therapistAvailabilityRepository
				.findByTherapistIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
						event.getTherapistId(), AvailabilityStatus.BOOKED, end, start);

		if (!bookedSlots.isEmpty()) {
			logger.warn("AvailabilitySlotsGenerated for therapistId={} range {} to {} arrived with {} BOOKED slot(s) in range; "
					+ "keeping them and skipping any overlapping incoming slots",
					event.getTherapistId(), start, end, bookedSlots.size());
		}

		therapistAvailabilityRepository.deleteAvailableSlotsInRange(event.getTherapistId(), start, end);

		List<TherapistAvailability> therapistAvailabilityList = new ArrayList<>();

		for (Slot slot : event.getSlotList()) {

			if (therapistAvailabilityRepository.existsBySlotId(slot.getSlotId())) {
				continue;
			}

			if (overlapsAny(slot.getStartTime(), slot.getEndTime(), bookedSlots)) {
				logger.warn("Skipping incoming slotId={} ({} - {}) — overlaps a BOOKED slot. therapistId={}",
						slot.getSlotId(), slot.getStartTime(), slot.getEndTime(), event.getTherapistId());
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

	/**
	 * Deletes AVAILABLE slots in range. BOOKED rows are untouched by design;
	 * the producer refuses range deletes over active appointments, so a BOOKED
	 * slot here is the same edge case as in processBatchGeneration and is
	 * simply kept.
	 */
	public void processBatchDeletion(AvailabilitySlotsDeletedEvent event) {

		logger.debug("inside processBatchDeletion..");

		LocalDateTime start = event.getRangeStart().atStartOfDay();
		LocalDateTime end = event.getRangeEnd().plusDays(1).atStartOfDay();

		boolean hasBookedSlots = therapistAvailabilityRepository.existsByTherapistIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
				event.getTherapistId(),
				AvailabilityStatus.BOOKED,
				end,
				start);

		if (hasBookedSlots) {
			logger.warn("AvailabilitySlotsDeleted for therapistId={} range {} to {} has BOOKED slot(s) in range; "
					+ "deleting AVAILABLE slots only, BOOKED slots kept",
					event.getTherapistId(), start, end);
		}

		therapistAvailabilityRepository.deleteAvailableSlotsInRange(event.getTherapistId(), start, end);
	}

	private boolean overlapsAny(LocalDateTime start, LocalDateTime end, List<TherapistAvailability> bookedSlots) {
		for (TherapistAvailability booked : bookedSlots) {
			if (start.isBefore(booked.getEndTime()) && end.isAfter(booked.getStartTime())) {
				return true;
			}
		}
		return false;
	}
}
