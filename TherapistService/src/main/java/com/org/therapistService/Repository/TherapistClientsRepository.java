package com.org.therapistService.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.events.Client.ClientStatus;
import com.org.therapistService.Entity.TherapistClients;

@Repository
public interface TherapistClientsRepository extends JpaRepository<TherapistClients, String>{

	List<TherapistClients> findByTherapistId(String therapistId);
	Page<TherapistClients> findByTherapistId(String therapistId, Pageable pageable);
	Optional<TherapistClients> findByTherapistIdAndClientId(String therapistId, String clientId);
	long countByTherapistIdAndStatus(String therapistId, ClientStatus status);
}
