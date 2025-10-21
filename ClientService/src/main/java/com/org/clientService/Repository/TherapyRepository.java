package com.org.clientService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.clientService.Entity.Therapist;

@Repository
public interface TherapyRepository extends JpaRepository<Therapist, Long>{

}
