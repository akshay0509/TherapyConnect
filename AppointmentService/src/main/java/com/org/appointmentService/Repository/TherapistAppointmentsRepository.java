package com.org.appointmentService.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.appointmentService.Entity.TherapistAppointments;

@Repository
public interface TherapistAppointmentsRepository extends JpaRepository<TherapistAppointments, String>{

	List<TherapistAppointments> findByTherapistIdAndStartTimeBetweenOrderByStartTimeAsc(
			String therapistId,
			LocalDateTime startTime,
			LocalDateTime endTime
			);
}
