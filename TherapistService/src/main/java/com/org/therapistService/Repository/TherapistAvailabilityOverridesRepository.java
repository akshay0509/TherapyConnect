package com.org.therapistService.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.therapistService.Entity.TherapistAvailabilityOverrides;

@Repository
public interface TherapistAvailabilityOverridesRepository extends JpaRepository<TherapistAvailabilityOverrides, String>{

	List<TherapistAvailabilityOverrides> findByTherapistIdAndStartTimeBetween(String therapistId, LocalDateTime start, LocalDateTime end);
}
