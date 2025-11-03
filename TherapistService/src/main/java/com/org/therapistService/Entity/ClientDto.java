package com.org.therapistService.Entity;

import java.sql.Date;

import lombok.Data;

@Data
public class ClientDto {
	
	private String firstName;
	private String lastName;
	private Date dob;
	private String phoneNumber;
	private String emergencyPhoneNumber;
	private String email;
	private String pronouns;
	private String gender;
	private String modeOfSession;
	private int preferredDay;
}
