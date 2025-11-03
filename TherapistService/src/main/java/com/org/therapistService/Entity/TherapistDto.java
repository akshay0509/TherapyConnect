package com.org.therapistService.Entity;


import java.sql.Date;

import lombok.Data;

@Data
public class TherapistDto {

	private String firstName;
	private String lastName;
	private Date dob;
	private String phoneNumber;
	private String email;
	private String gender;
	private int yearsOfExperience;
}
