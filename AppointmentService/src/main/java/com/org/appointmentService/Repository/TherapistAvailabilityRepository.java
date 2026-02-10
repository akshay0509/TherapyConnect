package com.org.appointmentService.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.org.appointmentService.Entity.TherapistAvailability;

@Repository
public interface TherapistAvailabilityRepository extends JpaRepository<TherapistAvailability, String>{

	List<TherapistAvailability> findByTherapistId(String therapistId);

	boolean existsBySlotId(String slotId);

	TherapistAvailability findBySlotId(String slotId);

	@Modifying
	@Query("""
			    UPDATE AvailabilitySlot s
			    SET s.status = 'BOOKED'
			    WHERE s.slotId = :slotId
			      AND s.status = 'AVAILABLE'
			""")
	int markSlotAsBooked(String slotId);

}
