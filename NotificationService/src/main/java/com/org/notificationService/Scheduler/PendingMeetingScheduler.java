package com.org.notificationService.Scheduler;
import java.time.LocalDateTime;
import com.org.notificationService.Repository.AppointmentMeetingStateRepository;
import com.org.notificationService.Services.PendingMeetingResolver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class PendingMeetingScheduler {
    private static final Logger log = LoggerFactory.getLogger(PendingMeetingScheduler.class);
    private final AppointmentMeetingStateRepository repository;
    private final PendingMeetingResolver resolver;
    public PendingMeetingScheduler(AppointmentMeetingStateRepository repository, PendingMeetingResolver resolver) {
        this.repository = repository; this.resolver = resolver;
    }
    @Scheduled(fixedDelay = 5000)
    public void resolvePending() {
        for (var state : repository.findTop25ByMeetingStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc("PENDING", LocalDateTime.now())) {
            try { resolver.resolve(state.getAppointmentId()); }
            catch (Exception ex) { log.warn("Meeting resolution transaction failed appointmentId={}", state.getAppointmentId()); }
        }
    }
}
