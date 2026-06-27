package com.org.clientService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.clientService.Entity.Client;
import com.org.events.Client.ClientStatus;

@Repository
public interface ClientRepository  extends JpaRepository<Client, String>{

	Client findByTherapistIdAndClientId(String therapistId, String clientId);
	long countByTherapistIdAndStatus(String therapistId, ClientStatus status);
}
