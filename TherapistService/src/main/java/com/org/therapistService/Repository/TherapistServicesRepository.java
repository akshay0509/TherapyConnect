package com.org.therapistService.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.therapistService.Entity.TherapistServices;

@Repository
public interface TherapistServicesRepository extends JpaRepository<TherapistServices, String>{

	List<TherapistServices> findByTherapistId(
			String therapistId
	       );
	
	List<TherapistServices> findByTherapistIdAndIsActiveTrue(String therapistId);
}
