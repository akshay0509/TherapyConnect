package com.org.therapistService.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.org.therapistService.Entity.TherapistAvailability;

@Repository
public interface TherapistAvailabilityRepository extends JpaRepository<TherapistAvailability, String>{

	boolean existsByTherapistIdAndStartTime(String therapistId, LocalDateTime startTime);

	List<TherapistAvailability> findByTherapistId(
			String therapistId
			);

	@Modifying
	@Query("""
			    DELETE FROM TherapistAvailability a
			    WHERE a.therapistId = :therapistId
			      AND a.startTime >= :startTime
			      AND a.startTime < :endTime
			""")
	void deleteInRange(
			String therapistId,
			LocalDateTime startTime,
			LocalDateTime endTime
			);
}
