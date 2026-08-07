package com.org.therapistService.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.therapistService.Entity.ClientRisk;

public interface ClientRiskRepository extends JpaRepository<ClientRisk, String> {

	/** Scoped by therapist as well as client — a client id alone is not an
	 *  authorisation boundary. */
	Optional<ClientRisk> findByTherapistIdAndClientId(String therapistId, String clientId);
}
