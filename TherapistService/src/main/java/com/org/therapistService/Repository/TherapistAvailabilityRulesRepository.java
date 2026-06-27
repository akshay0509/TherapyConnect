package com.org.therapistService.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
	
	List<TherapistAvailabilityRules> findByTherapistIdAndIsActiveTrue(
			String therapistId
			);
	
	List<TherapistAvailabilityRules> findByTherapistId(
			String therapistId
			);
	
	Page<TherapistAvailabilityRules> findByTherapistId(String therapistId, Pageable pageable);
	Optional<TherapistAvailabilityRules> findByRuleIdAndTherapistId(String ruleId, String therapistId);
	
	@Query("SELECT DISTINCT r.therapistId FROM TherapistAvailabilityRules r")
    List<String> findAllDistinctTherapistIds();
}
