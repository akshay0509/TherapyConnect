package com.org.therapistService.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.therapistService.Entity.TherapistAvailabilityOverrides;

public interface TherapistAvailabilityOverridesRepository extends JpaRepository<TherapistAvailabilityOverrides, String>{

	List<TherapistAvailabilityOverrides> findByTherapistIdAndStartTimeBetween(Long therapistId, LocalDateTime start, LocalDateTime end);
}
