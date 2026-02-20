package com.org.therapistService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.therapistService.Entity.Therapist;

@Repository
public interface TherapistRepository extends JpaRepository<Therapist, String>{

	Therapist findByUserId(String userId);
	
	Therapist findByTherapistId(String therapistId);
}
