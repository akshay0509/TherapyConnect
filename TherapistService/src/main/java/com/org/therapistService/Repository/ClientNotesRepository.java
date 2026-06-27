package com.org.therapistService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.therapistService.Entity.ClientNotes;

@Repository
public interface ClientNotesRepository extends JpaRepository<ClientNotes, String> {

	ClientNotes findByTherapistIdAndClientId(String therapistId, String clientId);
}
