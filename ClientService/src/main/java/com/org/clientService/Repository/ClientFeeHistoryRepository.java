package com.org.clientService.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.clientService.Entity.ClientFeeHistory;

public interface ClientFeeHistoryRepository extends JpaRepository<ClientFeeHistory, String> {

	/** Scoped by therapist as well as client — the client id alone is not an
	 *  authorisation boundary. */
	List<ClientFeeHistory> findByTherapistIdAndClientIdOrderByChangedAtDesc(String therapistId, String clientId);
}
