package com.org.therapistService.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.therapistService.Entity.TherapistAppointments;
import com.org.therapistService.Enums.AppointmentStatus;

@Repository
public interface TherapistAppointmentsRepository extends JpaRepository<TherapistAppointments, String>{

	List<TherapistAppointments> findByReminderSentFalseAndStatusAndStartTimeBetween(
			AppointmentStatus status,
	        LocalDateTime start,
	        LocalDateTime end
			);
	
	
}
