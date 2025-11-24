package com.org.therapistService.Repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.therapistService.Entity.TherapistAvailability;

public interface TherapistAvailabilityRepository extends JpaRepository<TherapistAvailability, String>{

	boolean existsByTherapistIdAndStartTime(String therapistId, LocalDateTime startTime);
}
