package com.org.therapistService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.therapistService.Entity.Therapist;

public interface TherapistRepository extends JpaRepository<Therapist, String>{

}
