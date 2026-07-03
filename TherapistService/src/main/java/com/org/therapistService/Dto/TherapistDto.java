package com.org.therapistService.Dto;


import java.sql.Date;

import lombok.Data;

@Data
public class TherapistDto {

	private String therapistId;
	private String firstName;
	private String lastName;
	private Date dob;
	private String phoneNumber;
	private String email;
	private String gender;
	private int yearsOfExperience;
	private String timezone;
	private Boolean paymentEnabled;
}
