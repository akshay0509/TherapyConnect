package com.org.notificationService.Services;

import java.net.URI;
import java.time.LocalDateTime;
import com.google.api.services.calendar.model.Event;
import com.org.events.TherapistAppointment.AppointmentEvent;
import com.org.notificationService.Entity.AppointmentMeetingState;
import com.org.notificationService.Repository.AppointmentMeetingStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingStateService {
    private final AppointmentMeetingStateRepository repository;
    private final MeetingOutboxService outbox;
    public MeetingStateService(AppointmentMeetingStateRepository repository, MeetingOutboxService outbox) {
        this.repository = repository; this.outbox = outbox;
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public AppointmentMeetingState begin(AppointmentEvent event) {
        repository.ensureRow(event.getAppointmentId());
        var state = repository.lockState(event.getAppointmentId()).orElseThrow();
        // Legacy events remain usable until a versioned event has been applied.
        long revision = event.getCalendarRevision() == null ? 0 : event.getCalendarRevision();
        if (revision < state.getCalendarRevision()
                || (revision > 0 && revision == state.getCalendarRevision())) return null;
        if (state.getTherapistId() != null && !state.getTherapistId().equals(event.getTherapistId()))
            throw new IllegalArgumentException("Meeting ownership mismatch");
        state.setTherapistId(event.getTherapistId()); state.setCalendarRevision(revision);
        state.setRetryCount(0); state.setNextAttemptAt(null); state.setUpdatedAt(LocalDateTime.now());
        return state;
    }
    public static String meetUrl(Event event) {
        if (event == null) return null;
        String url = event.getHangoutLink();
        if (valid(url)) return url;
        if (event.getConferenceData() != null && event.getConferenceData().getEntryPoints() != null)
            for (var entry : event.getConferenceData().getEntryPoints())
                if ("video".equals(entry.getEntryPointType()) && valid(entry.getUri())) return entry.getUri();
        return null;
    }
    private static boolean valid(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equals(uri.getScheme()) && "meet.google.com".equals(uri.getHost())
                && uri.getUserInfo() == null && uri.getPort() == -1
                && uri.getPath() != null && uri.getPath().matches("/[a-z]{3}-[a-z]{4}-[a-z]{3}");
        } catch (Exception ex) { return false; }
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void capture(AppointmentMeetingState state, Event googleEvent, String modeType) throws Exception {
        String previousStatus = state.getMeetingStatus();
        String previousUrl = state.getMeetingUrl();
        state.setGoogleCalendarEventId(googleEvent.getId());
        state.setMeetingUrl("ONLINE".equals(modeType) ? meetUrl(googleEvent) : null);
        String status = !"ONLINE".equals(modeType) ? "NOT_APPLICABLE" : state.getMeetingUrl() != null ? "READY" : "PENDING";
        if ("PENDING".equals(status) && googleEvent.getConferenceData() != null
                && googleEvent.getConferenceData().getCreateRequest() != null
                && googleEvent.getConferenceData().getCreateRequest().getStatus() != null
                && "failure".equals(googleEvent.getConferenceData().getCreateRequest().getStatus().getStatusCode())) status = "FAILED";
        state.setMeetingStatus(status);
        state.setNextAttemptAt("PENDING".equals(status) ? LocalDateTime.now().plusSeconds(5) : null);
        if ("PENDING".equals(status) && "PENDING".equals(previousStatus)
                && java.util.Objects.equals(previousUrl, state.getMeetingUrl()) && state.getRetryCount() > 0) {
            state.setUpdatedAt(LocalDateTime.now()); repository.save(state);
        } else save(state);
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void clear(AppointmentMeetingState state) throws Exception {
        state.setMeetingStatus("NOT_APPLICABLE"); state.setMeetingUrl(null); state.setNextAttemptAt(null); save(state);
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(AppointmentMeetingState state) throws Exception {
        state.setUpdatedAt(LocalDateTime.now()); outbox.saveOutboxEvent(state); repository.save(state);
    }
}
