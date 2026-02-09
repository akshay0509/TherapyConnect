package com.org.therapistService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.therapistService.Entity.SessionNotes;

@Repository
public interface SessionNotesRepository extends JpaRepository<SessionNotes, String>{

}
