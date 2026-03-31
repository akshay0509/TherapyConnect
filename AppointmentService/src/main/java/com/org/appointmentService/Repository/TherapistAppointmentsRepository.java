package com.org.appointmentService.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.appointmentService.Entity.TherapistAppointments;
import com.org.events.TherapistAppointment.AppointmentStatus;

@Repository
public interface TherapistAppointmentsRepository extends JpaRepository<TherapistAppointments, String>{

	List<TherapistAppointments> findByTherapistIdAndStatusInAndStartTimeBetweenOrderByStartTimeAsc(
			String therapistId,
			Collection<AppointmentStatus> statuses,
			LocalDateTime startTime,
			LocalDateTime endTime
			);
	
	Optional<TherapistAppointments> findByAppointmentIdAndTherapistId(String appointmentId, String therapistId);
}
