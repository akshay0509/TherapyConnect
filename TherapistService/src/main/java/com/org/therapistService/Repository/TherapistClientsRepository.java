package com.org.therapistService.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.therapistService.Entity.TherapistClients;

@Repository
public interface TherapistClientsRepository extends JpaRepository<TherapistClients, String>{

	List<TherapistClients> findByTherapistId(String therapistId);
}
