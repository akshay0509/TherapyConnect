package com.org.therapistService.Entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "SESSION_NOTES")
public class SessionNotes {

	@Id
	private String noteId;
	
	@Column(nullable = false)
	private String therapistId;
	
	@Column(nullable = false, unique = true)
	private String appointmentId;
	
	@Column(nullable = false)
	private String clientId;

	@Lob
	@Column(nullable = false)
    private String noteContent;
	
	@PrePersist
    public void generateId() {
        if (this.noteId == null) {
            String uniquePart = UUID.randomUUID().toString().substring(0, 8);
            this.noteId = "NOT" + uniquePart;
        }
    }
}
