package com.org.therapistService.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.org.events.TherapistAppointment.AppointmentStatus;
import com.org.therapistService.Entity.AppointmentProjection;

@Repository
public interface AppointmentProjectionRepository extends JpaRepository<AppointmentProjection, String>{

	AppointmentProjection findByAppointmentIdAndTherapistId(
			String appointmentId,
			String therapistId
			);

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

	@Query("""
			SELECT SUM(a.sessionFee)
			FROM AppointmentProjection a
			JOIN TherapistClients c ON c.therapistId = a.therapistId AND c.clientId = a.clientId
			WHERE a.therapistId = :therapistId
				AND a.status = com.org.events.TherapistAppointment.AppointmentStatus.COMPLETED
				AND c.dsf = false
				AND a.startTime >= :start
				AND a.startTime < :end
			""")
	BigDecimal sumPaidCompletedEarningsBetween(
			String therapistId,
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
			LocalDateTime startTime
			);

}
