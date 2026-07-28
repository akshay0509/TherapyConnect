package com.org.appointmentService.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

	List<TherapistAppointments> findByTherapistIdAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
			String therapistId,
			LocalDateTime endTime,
			LocalDateTime startTime
			);

	@Query("""
			SELECT COUNT(a) > 0
			FROM TherapistAppointments a
			WHERE a.therapistId = :therapistId
			AND (:excludeAppointmentId IS NULL OR a.appointmentId <> :excludeAppointmentId)
			AND a.status IN (
			com.org.events.TherapistAppointment.AppointmentStatus.SCHEDULED,
			com.org.events.TherapistAppointment.AppointmentStatus.CONFIRMED,
			com.org.events.TherapistAppointment.AppointmentStatus.RESCHEDULED
			)
			AND a.startTime < :endTime
			AND a.endTime > :startTime
			""")
	boolean existsActiveAppointmentOverlap(
			String therapistId,
			String excludeAppointmentId,
			LocalDateTime startTime,
			LocalDateTime endTime
			);

}
