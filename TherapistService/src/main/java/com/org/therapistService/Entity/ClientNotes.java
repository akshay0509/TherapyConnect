package com.org.therapistService.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.org.therapistService.Utility.SessionNotesEncryptor;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Data
@Table(
	    name = "CLIENT_NOTES",
	    uniqueConstraints = @UniqueConstraint(columnNames = {"therapistId", "clientId"})
	)
public class ClientNotes {

	@Id
	private String noteId;

	@Column(nullable = false)
	private String clientId;

	@Column(nullable = false)
	private String therapistId;

	@Convert(converter = SessionNotesEncryptor.class)
	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
	
	@Column(nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();

	@PrePersist
	public void generateId() {
		if (this.noteId == null) {
			String uniquePart = UUID.randomUUID().toString().substring(0, 8);
			this.noteId = "CNT" + uniquePart;
		}
	}
}
