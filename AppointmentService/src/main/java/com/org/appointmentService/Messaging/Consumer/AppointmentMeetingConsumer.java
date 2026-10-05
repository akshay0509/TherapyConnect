package com.org.appointmentService.Messaging.Consumer;

import java.net.URI;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.events.TherapistAppointment.AppointmentMeetingUpdated;
import com.org.appointmentService.Repository.TherapistAppointmentsRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

@Service
public class AppointmentMeetingConsumer {
    private final ObjectMapper mapper;
    private final TherapistAppointmentsRepository appointments;
    public AppointmentMeetingConsumer(ObjectMapper mapper, TherapistAppointmentsRepository appointments) {
        this.mapper = mapper; this.appointments = appointments;
    }
    public static boolean validMeetUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equals(uri.getScheme()) && "meet.google.com".equals(uri.getHost())
                    && uri.getUserInfo() == null && uri.getPort() == -1
                    && uri.getPath() != null && uri.getPath().matches("/[a-z]{3}-[a-z]{4}-[a-z]{3}");
        } catch (Exception e) { return false; }
    }
    @KafkaListener(topics = "appointment-meeting-events", groupId = "appointment-meeting-projection-group")
    @Transactional
    public void listen(JsonNode payload) {
        if (!"AppointmentMeetingUpdated".equals(payload.path("eventType").asText())) return;
        AppointmentMeetingUpdated event = mapper.convertValue(payload, AppointmentMeetingUpdated.class);
        if (event.getAppointmentId() == null || event.getTherapistId() == null
                || event.getCalendarRevision() < 0 || event.getMeetingSequence() <= 0
                || event.getMeetingStatus() == null || !Set.of("READY", "PENDING", "FAILED", "NOT_APPLICABLE").contains(event.getMeetingStatus())) {
            throw new IllegalArgumentException("Invalid meeting event metadata");
        }
        String url = "READY".equals(event.getMeetingStatus()) ? event.getMeetingUrl() : null;
        if ("READY".equals(event.getMeetingStatus()) && !validMeetUrl(url))
            throw new IllegalArgumentException("Invalid Meet URL");
        // Only meeting fields change. Stale/duplicate/cross-tenant events update zero rows.
        appointments.applyMeetingUpdate(event.getAppointmentId(), event.getTherapistId(),
                event.getCalendarRevision(), event.getMeetingSequence(), event.getMeetingStatus(), url, event.isCalendarEligible());
    }
}
