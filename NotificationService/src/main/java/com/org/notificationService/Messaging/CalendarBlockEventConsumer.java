package com.org.notificationService.Messaging;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.events.TherapistAvailability.CalendarBlockEvent;
import com.org.notificationService.Entity.AvailabilityBlockCalendarEvent;
import com.org.notificationService.Repository.AvailabilityBlockCalendarEventRepository;
import com.org.notificationService.Services.GoogleCalendarService;

import jakarta.transaction.Transactional;

@Service
public class CalendarBlockEventConsumer {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @Autowired
    private AvailabilityBlockCalendarEventRepository availabilityBlockCalendarEventRepository;

    private static final String topic = "therapist-availability-events";

    private static final Logger logger = LoggerFactory.getLogger(CalendarBlockEventConsumer.class);

    @KafkaListener(topics = topic, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void listen(JsonNode payload) {
    	
        String eventType = payload.get("eventType").asText();

        switch (eventType) {
            case "CalendarBlockCreated" -> createBlock(objectMapper.convertValue(payload, CalendarBlockEvent.class));
            case "CalendarBlockDeleted" -> deleteBlock(objectMapper.convertValue(payload, CalendarBlockEvent.class));
            default -> logger.debug("Skipping unsupported availability eventType={}", eventType);
        }
        
    }

    private void createBlock(CalendarBlockEvent calendarBlockEvent) {
    	
        if (!Boolean.TRUE.equals(calendarBlockEvent.getSyncToGoogleCalendar())) {
            return;
        }

        Optional<AvailabilityBlockCalendarEvent> existing = availabilityBlockCalendarEventRepository.findById(calendarBlockEvent.getBlockId());
        
        if (existing.isPresent()) {
            logger.info("Google mapping already exists for blockId={}", calendarBlockEvent.getBlockId());
            return;
        }

        try {
            String googleCalendarEventId = googleCalendarService.createAvailabilityBlockEvent(
            		calendarBlockEvent.getReason() != null && !calendarBlockEvent.getReason().isBlank() ? calendarBlockEvent.getReason() : "Unavailable",
                    "Blocked time",
                    calendarBlockEvent.getStartTime(),
                    calendarBlockEvent.getEndTime());

            AvailabilityBlockCalendarEvent mapping = new AvailabilityBlockCalendarEvent();
            mapping.setBlockId(calendarBlockEvent.getBlockId());
            mapping.setGoogleCalendarEventId(googleCalendarEventId);
            availabilityBlockCalendarEventRepository.save(mapping);
        } catch (Exception e) {
            logger.error("Failed to create Google Calendar block for blockId={}", calendarBlockEvent.getBlockId(), e);
        }
    }

    private void deleteBlock(CalendarBlockEvent calendarBlockEvent) {
    	
        Optional<AvailabilityBlockCalendarEvent> mapping = availabilityBlockCalendarEventRepository.findById(calendarBlockEvent.getBlockId());
        if (mapping.isEmpty()) {
            logger.info("No Google Calendar mapping found for blockId={}", calendarBlockEvent.getBlockId());
            return;
        }

        try {
            googleCalendarService.deleteCalendarEvent(mapping.get().getGoogleCalendarEventId(), false);
            availabilityBlockCalendarEventRepository.delete(mapping.get());
        } catch (Exception e) {
            logger.error("Failed to delete Google Calendar block for blockId={}", calendarBlockEvent.getBlockId(), e);
        }
    }
}
