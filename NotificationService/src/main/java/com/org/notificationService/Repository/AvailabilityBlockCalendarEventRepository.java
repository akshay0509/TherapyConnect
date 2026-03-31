package com.org.notificationService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.notificationService.Entity.AvailabilityBlockCalendarEvent;

@Repository
public interface AvailabilityBlockCalendarEventRepository extends JpaRepository<AvailabilityBlockCalendarEvent, String> { // CODEX-CALENDAR-BLOCKING
}
