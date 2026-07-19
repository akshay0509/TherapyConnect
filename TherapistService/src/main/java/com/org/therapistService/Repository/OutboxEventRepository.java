package com.org.therapistService.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.therapistService.Entity.OutboxEvent;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

	List<OutboxEvent> findTop100ByPublishedFalseAndParkedFalseOrderByCreatedAtAsc();

	long countByParkedTrue();

	long deleteByPublishedTrueAndCreatedAtBefore(LocalDateTime cutoff);
}
