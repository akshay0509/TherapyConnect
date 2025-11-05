package com.org.therapistService.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.therapistService.Entity.TherapistServices;

public interface TherapistServicesRepository extends JpaRepository<TherapistServices, String>{

	List<TherapistServices> findByTherapistId(
			String therapistId
	       );
}
