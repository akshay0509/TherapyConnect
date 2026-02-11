package com.org.therapistService.Messaging.Consumer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.org.appointmentService.Entity.TherapistAvailability;
import com.org.appointmentService.Enums.AvailabilityStatus;
import com.org.appointmentService.Repository.TherapistAvailabilityRepository;
import com.org.events.TherapistAvailability.AvailabilityEvent;
import com.org.events.TherapistAvailability.AvailabilitySlotsGeneratedEvent;
import com.org.events.TherapistAvailability.Slot;

import jakarta.transaction.Transactional;

@Component
public class TherapistAvailabilityConsumer {

	@Autowired
	TherapistAvailabilityRepository therapistAvailabilityRepository;

	private static final String topic = "therapist.availability.events";

	@KafkaListener(topics = topic, groupId = "${spring.kafka.consumer.group-id}")
	@Transactional
	public void process(Object event) {

		if (event instanceof AvailabilitySlotsGeneratedEvent batchEvent) {
			processBatchGeneration(batchEvent);
			return;
		}

		if (event instanceof AvailabilityEvent singleEvent) {
			processSingleSlotEvent(singleEvent);
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
		therapistAvailability.setStartTime(event.getStartTime());
		therapistAvailability.setEndTime(event.getEndTime());
		therapistAvailability.setStatus(AvailabilityStatus.AVAILABLE);

		therapistAvailabilityRepository.save(therapistAvailability);
	}

	private void processSlotRemoved(AvailabilityEvent event) {

		TherapistAvailability therapistAvailability = therapistAvailabilityRepository.findBySlotId(event.getSlotId());

		if (therapistAvailability == null) {
			return;
		}

		if (AvailabilityStatus.BOOKED.equals(therapistAvailability.getStatus())) {
			return;
		}

		therapistAvailabilityRepository.delete(therapistAvailability);
	}

	private void processBatchGeneration(AvailabilitySlotsGeneratedEvent event) {

		LocalDateTime start = event.getRangeStart().atStartOfDay(ZoneOffset.UTC).toLocalDateTime();
		LocalDateTime end = event.getRangeEnd().plusDays(1).atStartOfDay(ZoneOffset.UTC).toLocalDateTime();


		therapistAvailabilityRepository.deleteAvailableSlotsInRange(event.getTherapistId(), start, end);
		
		if(event.getSlotList().size() != 0) {
			
			List<TherapistAvailability> therapistAvailabilityList = new ArrayList<>();
			
			for (Slot slot : event.getSlotList()) {

				if (therapistAvailabilityRepository.existsBySlotId(slot.getSlotId())) {
					continue;
				}

				TherapistAvailability therapistAvailability = new TherapistAvailability();
				therapistAvailability.setSlotId(slot.getSlotId());
				therapistAvailability.setTherapistId(event.getTherapistId());
				therapistAvailability.setStartTime(slot.getStartTime());
				therapistAvailability.setEndTime(slot.getEndTime());
				therapistAvailability.setStatus(AvailabilityStatus.AVAILABLE);
				
				therapistAvailabilityList.add(therapistAvailability);

			}
			therapistAvailabilityRepository.saveAll(therapistAvailabilityList);
		}
	}
}
