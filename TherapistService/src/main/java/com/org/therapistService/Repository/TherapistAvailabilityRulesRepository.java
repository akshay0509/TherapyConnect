package com.org.therapistService.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.org.therapistService.Entity.TherapistAvailabilityRules;

public interface TherapistAvailabilityRulesRepository extends JpaRepository<TherapistAvailabilityRules, String> {

	List<TherapistAvailabilityRules> findByTherapistIdAndDayOfWeekAndIsActiveTrue(
			Long therapistId,
			int dayOfWeek
			);
	
	@Query("SELECT DISTINCT r.therapistId FROM TherapistAvailabilityRules r")
    List<String> findAllDistinctTherapistIds();
}
