package com.org.notificationService.Services;
import java.time.LocalDateTime;
import com.org.notificationService.Repository.AppointmentMeetingStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PendingMeetingResolver {
    private static final Logger log = LoggerFactory.getLogger(PendingMeetingResolver.class);
    private final AppointmentMeetingStateRepository repository;
    private final GoogleCalendarService google;
    private final MeetingStateService states;
    public PendingMeetingResolver(AppointmentMeetingStateRepository repository, GoogleCalendarService google, MeetingStateService states) {
        this.repository = repository; this.google = google; this.states = states;
    }
    @Transactional(rollbackFor = Exception.class)
    public void resolve(String id) throws Exception {
        var state = repository.lockState(id).orElse(null);
        if (state == null || !"PENDING".equals(state.getMeetingStatus()) || state.getNextAttemptAt() == null
                || state.getNextAttemptAt().isAfter(LocalDateTime.now())) return;
        state.setRetryCount(state.getRetryCount() + 1);
        com.google.api.services.calendar.model.Event event = null;
        try {
            event = google.getAppointmentEvent(state.getGoogleCalendarEventId());
        } catch (Exception ex) {
            log.warn("Meeting resolution failed appointmentId={} attempt={}", id, state.getRetryCount());
            state.setMeetingStatus("PENDING");
        }
        if (event != null) states.capture(state, event, "ONLINE");
        if ("PENDING".equals(state.getMeetingStatus())) {
            if (state.getRetryCount() >= 12) {
                state.setMeetingStatus("FAILED"); state.setNextAttemptAt(null); states.save(state);
            } else {
                state.setNextAttemptAt(LocalDateTime.now().plusSeconds(Math.min(300, 5L << Math.min(state.getRetryCount(), 6))));
                repository.save(state);
            }
        }
    }
}
