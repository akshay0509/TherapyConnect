package com.org.appointmentService.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.org.appointmentService.Entity.OutboxEvent;

import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findTop100ByPublishedFalseAndParkedFalseOrderByCreatedAtAsc();

    long countByPublishedFalseAndParkedFalse();

    long countByParkedTrue();

    Optional<OutboxEvent> findTopByPublishedFalseAndParkedFalseOrderByCreatedAtAsc();

    long deleteByPublishedTrueAndCreatedAtBefore(LocalDateTime cutoff);

    // manual replay is also the un-park path: an admin explicitly requesting a
    // range gets parked events back into rotation with a fresh retry budget
    @Transactional
    @Modifying
    @Query("UPDATE OutboxEvent o SET o.published = false, o.parked = false, o.retryCount = 0 WHERE o.createdAt >= :from")
    int resetPublishedFrom(@Param("from") LocalDateTime from);
}
