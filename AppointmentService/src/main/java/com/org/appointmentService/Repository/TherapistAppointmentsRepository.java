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
			SELECT a
			FROM TherapistAppointments a
			WHERE a.therapistId = :therapistId
			AND (:clientName IS NULL OR LOWER(a.clientName) LIKE LOWER(CONCAT('%', :clientName, '%')))
			AND (:statusesEmpty = true OR a.status IN :statuses)
			AND a.startTime >= :from
			AND a.startTime < :to
			ORDER BY a.startTime ASC
			""")
	List<TherapistAppointments> searchAppointments(
			String therapistId,
			String clientName,
			boolean statusesEmpty,
			Collection<AppointmentStatus> statuses,
			LocalDateTime from,
			LocalDateTime to
			);
}
