package com.org.notificationService.Repository;
import com.org.notificationService.Entity.MeetingOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
public interface MeetingOutboxRepository extends JpaRepository<MeetingOutboxEvent, String> {
    List<MeetingOutboxEvent> findTop100ByPublishedFalseAndParkedFalseOrderByCreatedAtAsc();
    long deleteByPublishedTrueAndCreatedAtBefore(LocalDateTime before);
}
