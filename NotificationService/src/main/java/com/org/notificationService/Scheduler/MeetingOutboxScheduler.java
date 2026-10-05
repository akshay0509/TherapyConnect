package com.org.notificationService.Scheduler;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.notificationService.Repository.MeetingOutboxRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MeetingOutboxScheduler {
    private static final Logger log = LoggerFactory.getLogger(MeetingOutboxScheduler.class);
    private final MeetingOutboxRepository repository;
    private final ObjectMapper mapper;
    private final KafkaTemplate<String, JsonNode> kafka;
    public MeetingOutboxScheduler(MeetingOutboxRepository repository, ObjectMapper mapper, KafkaTemplate<String, JsonNode> kafka) {
        this.repository = repository; this.mapper = mapper; this.kafka = kafka;
    }
    @Scheduled(fixedDelay = 2000) @Transactional
    public void publish() {
        for (var row : repository.findTop100ByPublishedFalseAndParkedFalseOrderByCreatedAtAsc()) {
            try {
                kafka.send("appointment-meeting-events", row.getAppointmentId(), mapper.readTree(row.getPayload())).get(10, TimeUnit.SECONDS);
                row.setPublished(true);
            } catch (Exception ex) {
                row.setRetryCount(row.getRetryCount() + 1);
                row.setParked(row.getRetryCount() >= 150);
                log.warn("Meeting outbox publish failed id={} attempt={} parked={}", row.getEventId(), row.getRetryCount(), row.isParked());
                repository.save(row);
                if (!row.isParked()) break;
            }
            repository.save(row);
        }
    }
    @Scheduled(cron = "0 40 3 * * *") @Transactional
    public void purge() { repository.deleteByPublishedTrueAndCreatedAtBefore(LocalDateTime.now().minusDays(7)); }
}
