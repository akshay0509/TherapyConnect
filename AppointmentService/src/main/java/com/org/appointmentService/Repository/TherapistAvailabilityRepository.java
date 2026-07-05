package com.org.appointmentService.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.org.appointmentService.Dto.AvailabilityResponseDto;
import com.org.appointmentService.Entity.TherapistAvailability;
import com.org.appointmentService.Enums.AvailabilityStatus;

@Repository
public interface TherapistAvailabilityRepository extends JpaRepository<TherapistAvailability, String>{

	List<TherapistAvailability> findByTherapistId(String therapistId);

	boolean existsBySlotId(String slotId);

	Optional<TherapistAvailability> findBySlotIdAndTherapistId(String slotId, String therapistId);

	boolean existsByTherapistIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            String therapistId,
            AvailabilityStatus status,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    List<TherapistAvailability> findByTherapistIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            String therapistId,
            AvailabilityStatus status,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    List<TherapistAvailability> findTop10ByTherapistIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
            String therapistId,
            AvailabilityStatus status,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    // past AVAILABLE slots only — BOOKED rows anchor the calendar-history join
    long deleteByStatusAndEndTimeBefore(AvailabilityStatus status, LocalDateTime cutoff);

	@Modifying
	@Query("""
			    UPDATE TherapistAvailability s
			    SET s.status = 'BOOKED'
			    WHERE s.slotId = :slotId
			      AND s.status = 'AVAILABLE'
			""")
	int markSlotAsBooked(String slotId);

	@Modifying
	@Query("""
			    UPDATE TherapistAvailability s
			    SET s.status = 'AVAILABLE'
			    WHERE s.slotId = :slotId
			      AND s.status = 'BOOKED'
			""")
	int markSlotAsAvailable(String slotId);

	@Modifying
	@Query("""
			    DELETE FROM TherapistAvailability s
			    WHERE s.therapistId = :therapistId
			      AND s.status = 'AVAILABLE'
			      AND s.startTime >= :rangeStart
			      AND s.startTime < :rangeEnd
			""")
	void deleteAvailableSlotsInRange(
			String therapistId,
			LocalDateTime rangeStart,
			LocalDateTime rangeEnd
			);

	@Query("""
			SELECT new com.org.appointmentService.Dto.AvailabilityResponseDto(
			s.slotId,
			s.therapistId,
			s.serviceId,
			s.startTime,
			s.endTime,
			a.modeId,
			s.status,
			a.status,
			a.appointmentId,
			a.clientId,
			a.clientName
			)
			FROM TherapistAvailability s
			LEFT JOIN TherapistAppointments a
			ON s.slotId = a.slotId
			AND a.status IN (
			com.org.events.TherapistAppointment.AppointmentStatus.SCHEDULED,
			com.org.events.TherapistAppointment.AppointmentStatus.CONFIRMED,
			com.org.events.TherapistAppointment.AppointmentStatus.RESCHEDULED,
			com.org.events.TherapistAppointment.AppointmentStatus.COMPLETED,
			com.org.events.TherapistAppointment.AppointmentStatus.ABANDONED
			)
			WHERE s.therapistId = :therapistId
            AND NOT EXISTS (
                SELECT 1
                FROM TherapistAvailabilityOverride o
                WHERE o.therapistId = s.therapistId
                AND o.available = false
                AND o.startTime < s.endTime
                AND o.endTime > s.startTime
            )
            ORDER BY s.startTime
            """)
    List<AvailabilityResponseDto> findEffectiveSlotsWithAppointment(String therapistId);

	@Query("""
            SELECT new com.org.appointmentService.Dto.AvailabilityResponseDto(
            s.slotId,
            s.therapistId,
            s.serviceId,
            s.startTime,
            s.endTime,
            a.modeId,
            s.status,
            a.status,
            a.appointmentId,
            a.clientId,
            a.clientName
            )
            FROM TherapistAvailability s
            LEFT JOIN TherapistAppointments a
            ON s.slotId = a.slotId
            AND a.status IN (
            com.org.events.TherapistAppointment.AppointmentStatus.SCHEDULED,
            com.org.events.TherapistAppointment.AppointmentStatus.CONFIRMED,
            com.org.events.TherapistAppointment.AppointmentStatus.RESCHEDULED,
            com.org.events.TherapistAppointment.AppointmentStatus.COMPLETED,
            com.org.events.TherapistAppointment.AppointmentStatus.ABANDONED
            )
            WHERE s.therapistId = :therapistId
            AND s.startTime >= :rangeStart
            AND s.startTime < :rangeEnd
            AND NOT EXISTS (
                SELECT 1
                FROM TherapistAvailabilityOverride o
                WHERE o.therapistId = s.therapistId
                AND o.available = false
                AND o.startTime < s.endTime
                AND o.endTime > s.startTime
            )
            ORDER BY s.startTime
            """)
    List<AvailabilityResponseDto> findEffectiveSlotsWithAppointmentInRange(
            String therapistId,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    );

}
