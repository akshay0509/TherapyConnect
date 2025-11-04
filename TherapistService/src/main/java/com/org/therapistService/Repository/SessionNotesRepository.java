package com.org.therapistService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.therapistService.Entity.SessionNotes;

public interface SessionNotesRepository extends JpaRepository<SessionNotes, String>{

}
