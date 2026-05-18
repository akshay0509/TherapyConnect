package com.org.clientService.Entity;

import java.sql.Date;

import com.org.events.Client.ClientStatus;

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
	private String therapistId;
	private ClientStatus status;
	private Boolean dsf = false;
	//private String qualification;
	//private String currentOccupation;
}
