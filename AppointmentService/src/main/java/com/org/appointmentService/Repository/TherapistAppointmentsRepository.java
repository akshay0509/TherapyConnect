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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM TherapistAppointments a WHERE a.appointmentId = :appointmentId AND a.therapistId = :therapistId")
    Optional<TherapistAppointments> lockForCalendarUpdate(String appointmentId, String therapistId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE TherapistAppointments a SET a.meetingUrl = :url,
        a.meetingStatus = :meetingStatus, a.meetingSequence = :sequence,
        a.calendarEligible = CASE WHEN :eligible = true THEN true ELSE a.calendarEligible END
        WHERE a.appointmentId = :appointmentId AND a.therapistId = :therapistId
        AND COALESCE(a.calendarRevision, 0) = :revision
        AND COALESCE(a.meetingSequence, 0) < :sequence
        AND (:meetingStatus <> 'READY' OR EXISTS (SELECT m FROM TherapyDeliveryMode m
             WHERE m.modeId = a.modeId AND m.therapistId = a.therapistId
             AND m.modeType = com.org.appointmentService.Enums.DeliveryModeType.ONLINE))
        AND a.status IN (com.org.events.TherapistAppointment.AppointmentStatus.CONFIRMED,
                         com.org.events.TherapistAppointment.AppointmentStatus.RESCHEDULED)
        AND (a.calendarEligible = true OR a.calendarEligible IS NULL OR (COALESCE(a.calendarRevision, 0) = 0))
        """)
    int applyMeetingUpdate(String appointmentId, String therapistId, long revision,
                          long sequence, String meetingStatus, String url, boolean eligible);

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
