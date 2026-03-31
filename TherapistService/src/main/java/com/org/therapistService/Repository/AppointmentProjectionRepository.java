package com.org.therapistService.Repository;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.events.TherapistAppointment.AppointmentStatus;
import com.org.therapistService.Entity.AppointmentProjection;

@Repository
public interface AppointmentProjectionRepository extends JpaRepository<AppointmentProjection, String>{

	AppointmentProjection findByAppointmentIdAndTherapistId(String appointmentId, String therapistId);
	
	long countByTherapistIdAndStatusInAndStartTimeBetween(
			String therapistId,
			Collection<AppointmentStatus> statuses,
			LocalDateTime start,
			LocalDateTime end
	);

	long countByTherapistIdAndStatusAndStartTimeBetween(
			String therapistId,
			AppointmentStatus status,
			LocalDateTime start,
			LocalDateTime end
	);

	long countByTherapistIdAndStatusInAndStartTimeAfter(
			String therapistId,
			Collection<AppointmentStatus> statuses,
			LocalDateTime time
	);
	
	boolean existsByTherapistIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            String therapistId,
            Collection<AppointmentStatus> statuses,
            LocalDateTime endTime,
            LocalDateTime startTime);
	
}
