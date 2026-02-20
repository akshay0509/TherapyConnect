package com.org.appointmentService.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.org.appointmentService.Dto.AvailabilityResponseDto;
import com.org.appointmentService.Entity.TherapistAvailability;

@Repository
public interface TherapistAvailabilityRepository extends JpaRepository<TherapistAvailability, String>{

	List<TherapistAvailability> findByTherapistId(String therapistId);

	boolean existsBySlotId(String slotId);

	TherapistAvailability findBySlotId(String slotId);

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
			    a.sessionType,
			    s.status,
			    a.appointmentId,
			    a.clientId,
			    a.clientName
			)
			FROM TherapistAvailability s
			LEFT JOIN TherapistAppointments a
			ON s.slotId = a.slotId
			WHERE s.therapistId = :therapistId
			ORDER BY s.startTime
			""")
			List<AvailabilityResponseDto> findSlotsWithAppointment(String therapistId);

}
