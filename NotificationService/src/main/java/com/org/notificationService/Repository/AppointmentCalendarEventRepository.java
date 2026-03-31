package com.org.notificationService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.notificationService.Entity.AppointmentCalendarEvent;

@Repository
public interface AppointmentCalendarEventRepository extends JpaRepository<AppointmentCalendarEvent, String> {
}
