package com.org.notificationService.Messaging;

import java.time.ZoneId;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.org.events.Therapist.TherapistEvent;
import com.org.events.TherapistAvailability.CalendarBlockEvent;
import com.org.notificationService.Entity.AvailabilityBlockCalendarEvent;
import com.org.notificationService.Entity.TherapistProjection;
import com.org.notificationService.Repository.AvailabilityBlockCalendarEventRepository;
import com.org.notificationService.Repository.TherapistProjectionRepository;
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

    @Autowired
    private TherapistProjectionRepository therapistProjectionRepository;

    private static final String topic = "therapist-availability-events";
    private static final ZoneId FALLBACK_ZONE = ZoneId.of("Asia/Kolkata");

    private static final Logger logger = LoggerFactory.getLogger(CalendarBlockEventConsumer.class);

    // Failures must propagate so the DefaultErrorHandler can retry and
    // dead-letter them to <topic>.DLT — see AppointmentEventConsumer
    @KafkaListener(topics = topic, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void listen(JsonNode payload) throws Exception {

        String eventType = payload.get("eventType").asText();

        switch (eventType) {
            case "TherapistCreated", "TherapistPaymentSettingsUpdated" -> upsertProjection(objectMapper.convertValue(payload, TherapistEvent.class));
            case "CalendarBlockCreated" -> createBlock(objectMapper.convertValue(payload, CalendarBlockEvent.class));
            case "CalendarBlockDeleted" -> deleteBlock(objectMapper.convertValue(payload, CalendarBlockEvent.class));
            default -> logger.debug("Skipping unsupported availability eventType={}", eventType);
        }

    }

    private void upsertProjection(TherapistEvent event) {
        TherapistProjection projection = therapistProjectionRepository
                .findById(event.getTherapistId())
                .orElse(new TherapistProjection());
        projection.setTherapistId(event.getTherapistId());
        projection.setTimezone(event.getTimezone());
        // replayed events from before the email field existed carry no email —
        // never wipe a value that is already projected
        if (event.getEmail() != null && !event.getEmail().isBlank()) {
            projection.setEmail(event.getEmail());
        }
        therapistProjectionRepository.save(projection);
        logger.info("Upserted TherapistProjection for therapistId={} timezone={} email={}", event.getTherapistId(), event.getTimezone(), projection.getEmail());
    }

    private void createBlock(CalendarBlockEvent calendarBlockEvent) throws Exception {

        if (!Boolean.TRUE.equals(calendarBlockEvent.getSyncToGoogleCalendar())) {
            return;
        }

        Optional<AvailabilityBlockCalendarEvent> existing = availabilityBlockCalendarEventRepository.findById(calendarBlockEvent.getBlockId());

        if (existing.isPresent()) {
            logger.info("Google mapping already exists for blockId={}", calendarBlockEvent.getBlockId());
            return;
        }

        ZoneId zone = therapistProjectionRepository.findById(calendarBlockEvent.getTherapistId())
                .map(p -> {
                    try { return ZoneId.of(p.getTimezone()); } catch (Exception e) { return FALLBACK_ZONE; }
                })
                .orElse(FALLBACK_ZONE);

        String googleCalendarEventId = googleCalendarService.createAvailabilityBlockEvent(
        		calendarBlockEvent.getReason() != null && !calendarBlockEvent.getReason().isBlank() ? calendarBlockEvent.getReason() : "Unavailable",
                "Blocked time",
                calendarBlockEvent.getStartTime(),
                calendarBlockEvent.getEndTime(),
                zone);

        AvailabilityBlockCalendarEvent mapping = new AvailabilityBlockCalendarEvent();
        mapping.setBlockId(calendarBlockEvent.getBlockId());
        mapping.setGoogleCalendarEventId(googleCalendarEventId);
        availabilityBlockCalendarEventRepository.save(mapping);
    }

    private void deleteBlock(CalendarBlockEvent calendarBlockEvent) throws Exception {

        Optional<AvailabilityBlockCalendarEvent> mapping = availabilityBlockCalendarEventRepository.findById(calendarBlockEvent.getBlockId());
        if (mapping.isEmpty()) {
            logger.info("No Google Calendar mapping found for blockId={}", calendarBlockEvent.getBlockId());
            return;
        }

        try {
            googleCalendarService.deleteCalendarEvent(mapping.get().getGoogleCalendarEventId(), false);
        } catch (GoogleJsonResponseException e) {
            // already deleted on Google's side — treat as success, clean up mapping
            if (e.getStatusCode() != 404 && e.getStatusCode() != 410) {
                throw e;
            }
            logger.warn("Calendar block already gone for blockId={}; removing mapping", calendarBlockEvent.getBlockId());
        }
        availabilityBlockCalendarEventRepository.delete(mapping.get());
    }
}
