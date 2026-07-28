package com.org.appointmentService.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.appointmentService.Entity.TherapistServiceProjection;

@Repository
public interface TherapistServiceProjectionRepository
		extends JpaRepository<TherapistServiceProjection, String> {

	Optional<TherapistServiceProjection> findByServiceIdAndTherapistIdAndActiveTrue(
			String serviceId,
			String therapistId);
}
