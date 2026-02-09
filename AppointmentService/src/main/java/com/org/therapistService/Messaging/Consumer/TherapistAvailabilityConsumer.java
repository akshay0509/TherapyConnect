package com.org.therapistService.Messaging.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.org.appointmentService.Entity.TherapistAvailability;
import com.org.appointmentService.Enums.AvailabilityStatus;
import com.org.appointmentService.Repository.TherapistAvailabilityRepository;
import com.org.events.TherapistAvailability.AvailabilityEvent;

import jakarta.transaction.Transactional;

@Component
public class TherapistAvailabilityConsumer {

	@Autowired
	TherapistAvailabilityRepository therapistAvailabilityRepository;
	
	private static final String topic = "therapist.availability.events";
		
	@KafkaListener(topics = topic, groupId = "${spring.kafka.consumer.group-id}")
	@Transactional
	public void createSlot(AvailabilityEvent event) {
		
		if ("AvailabilitySlotCreated".equals(event.getEventType())) {
            handleSlotCreated(event);
            return;
        }

        if ("AvailabilitySlotRemoved".equals(event.getEventType())) {
            handleSlotRemoved(event);
            return;
        }
	}
	
	private void handleSlotCreated(AvailabilityEvent event) {

        if (therapistAvailabilityRepository.existsBySlotId(event.getSlotId())) {
            return; // idempotent
        }

        TherapistAvailability slot = new TherapistAvailability();
        slot.setSlotId(event.getSlotId());
        slot.setTherapistId(event.getTherapistId());
        slot.setStartTime(event.getStartTime());
        slot.setEndTime(event.getEndTime());
        slot.setStatus(AvailabilityStatus.AVAILABLE);
        slot.setGenerationId(event.getGenerationId());

        therapistAvailabilityRepository.save(slot);
    }
	
	private void handleSlotRemoved(AvailabilityEvent event) {

        TherapistAvailability therapistAvailability = therapistAvailabilityRepository.findBySlotId(event.getSlotId());

        if (therapistAvailability == null) {
            return; // already removed
        }

        if (AvailabilityStatus.BOOKED.equals(therapistAvailability.getStatus())) {
            return; // appointment owns it now
        }

        therapistAvailabilityRepository.delete(therapistAvailability);
    }
}
