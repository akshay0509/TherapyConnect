package com.org.therapistService.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.org.therapistService.Dto.SessionDetailsDto;
import com.org.therapistService.Entity.SessionNotes;

@Repository
public interface SessionNotesRepository extends JpaRepository<SessionNotes, String>{

	SessionNotes findByAppointmentId(String appointmentId);

	SessionNotes findByAppointmentIdAndTherapistId(String appointmentId, String therapistId);

	List<SessionNotes> findByClientIdOrderByCreatedAtDesc(String clientId);
	
	@Query("""
	        SELECT new com.org.therapistService.Dto.SessionDetailsDto(
	            a.appointmentId,
	            a.clientId,
	            a.startTime,
	            a.endTime,
	            a.status,
	            a.modeId,
	            s.noteContent
	        )
	        FROM AppointmentProjection a
	        LEFT JOIN SessionNotes s
	            ON a.appointmentId = s.appointmentId
	        WHERE a.therapistId = :therapistId
	          AND a.clientId = :clientId
	        ORDER BY a.startTime DESC
	    """)
	    List<SessionDetailsDto> findAppointmentsWithNotes(String therapistId, String clientId);
}
