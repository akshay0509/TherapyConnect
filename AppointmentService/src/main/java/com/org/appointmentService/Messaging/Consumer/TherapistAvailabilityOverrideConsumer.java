package com.org.appointmentService.Messaging.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.appointmentService.Entity.TherapistAvailabilityOverride;
import com.org.appointmentService.Repository.TherapistAvailabilityOverrideRepository;
import com.org.events.TherapistAvailability.AvailabilityOverrideEvent;

import jakarta.transaction.Transactional;

@Component
public class TherapistAvailabilityOverrideConsumer {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TherapistAvailabilityOverrideRepository therapistAvailabilityOverrideRepository;
    
    private static final String topic = "therapist-availability-events";

    @KafkaListener(topics = topic, groupId = "appointment-availability-override-projection-group")
    @Transactional
    public void process(JsonNode payload) {
        String eventType = payload.get("eventType").asText();
        switch (eventType) {
            case "AvailabilityOverrideCreated" -> handleCreated(objectMapper.convertValue(payload, AvailabilityOverrideEvent.class));
            case "AvailabilityOverrideDeleted" -> handleDeleted(objectMapper.convertValue(payload, AvailabilityOverrideEvent.class));
            default -> { }
        }
    }

    private void handleCreated(AvailabilityOverrideEvent event) {
        TherapistAvailabilityOverride override = new TherapistAvailabilityOverride();
        override.setOverrideId(event.getOverrideId());
        override.setTherapistId(event.getTherapistId());
        override.setStartTime(event.getStartTime());
        override.setEndTime(event.getEndTime());
        override.setAvailable(event.isAvailable());
        override.setReason(event.getReason());
        therapistAvailabilityOverrideRepository.save(override);
    }

    private void handleDeleted(AvailabilityOverrideEvent event) {
    	if (!therapistAvailabilityOverrideRepository.existsById(event.getOverrideId())) {
            return;
        }
        therapistAvailabilityOverrideRepository.deleteById(event.getOverrideId());
    }
}
