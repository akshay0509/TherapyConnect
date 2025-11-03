package com.org.therapistService.Entity;


import java.sql.Date;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPIST")
public class Therapist {

	@Id
	private String therapistId;
	
	private String firstName;
	private String lastName;
	private Date dob;
	private String phoneNumber;
	private String email;
	private String gender;
	private int yearsOfExperience;
	
	@PrePersist
    public void generateId() {
        if (this.therapistId == null) {
            String uniquePart = UUID.randomUUID().toString().substring(0, 8);
            this.therapistId = "THP" + uniquePart;
        }
    }
}
