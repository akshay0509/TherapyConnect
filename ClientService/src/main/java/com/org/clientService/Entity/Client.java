package com.org.clientService.Entity;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "CLIENTS")
public class Client {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	private String firstName;
	private String lastName;
	private Date dob;
	private int age;
	private Address address;
	private int phoneNumber;
	private int emergencyPhoneNumber;
	private String email;
	private String pronouns;
	private String gender;
	private String qualification;
	private String currentOccupation;
	private ModesOfSession modeOfSession;
	private Day preferredDay;
	
	private enum ModesOfSession{
		ONLINE,
		OFFLINE_AT_HALUSURU,
		OFFLINE_AT_SESHADRIPURAM
	}
	
	private enum Day {
	    MONDAY,
	    TUESDAY,
	    WEDNESDAY,
	    THURSDAY,
	    FRIDAY,
	    SATURDAY,
	    SUNDAY
	}
}
