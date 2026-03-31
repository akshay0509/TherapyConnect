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
	
	boolean existsByTherapistIdAndServiceIdAndStartTimeAndEndTime(
            String therapistId,
            String serviceId,
            LocalDateTime startTime,
            LocalDateTime endTime);

	List<TherapistAvailability> findByTherapistId(String therapistId);
	
	List<TherapistAvailability> findByTherapistIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
	        String therapistId,
	        LocalDateTime startTime,
	        LocalDateTime endTime
	);
	
	List<TherapistAvailability> findByTherapistIdAndStartTimeLessThanAndEndTimeGreaterThan(
            String therapistId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

	@Modifying
	@Query("""
			    DELETE FROM TherapistAvailability a
			    WHERE a.therapistId = :therapistId
			      AND a.startTime >= :startTime
			      AND a.startTime < :endTime
			""")
	void deleteInRange(String therapistId, LocalDateTime startTime, LocalDateTime endTime);
	
	@Modifying
    @Query("""
                DELETE FROM TherapistAvailability a
                WHERE a.therapistId = :therapistId
                  AND a.startTime < :endTime
                  AND a.endTime > :startTime
            """)
    void deleteOverlappingSlots(String therapistId, LocalDateTime startTime, LocalDateTime endTime);
}
