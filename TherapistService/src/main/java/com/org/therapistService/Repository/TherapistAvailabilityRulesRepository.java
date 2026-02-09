package com.org.therapistService.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.org.therapistService.Entity.TherapistAvailabilityRules;

@Repository
public interface TherapistAvailabilityRulesRepository extends JpaRepository<TherapistAvailabilityRules, String> {

	List<TherapistAvailabilityRules> findByTherapistIdAndDayOfWeekAndIsActiveTrue(
			String therapistId,
			int dayOfWeek
			);
	
	@Query("SELECT DISTINCT r.therapistId FROM TherapistAvailabilityRules r")
    List<String> findAllDistinctTherapistIds();
}
