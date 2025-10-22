package com.org.clientService.Entity;

import java.sql.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "CLIENT")
public class Client {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column(name = "firstname")
	private String firstName;
	
	@Column(name = "lastname")
	private String lastName;

	@Column(name = "dob")
	private Date dob;

	@Column(name = "age")
	private int age;
	//private Address address;

	@Column(name = "phonenumber")
	private String phoneNumber;

	@Column(name = "emergencyphonenumber")
	private String emergencyPhoneNumber;

	@Column(name = "email")
	private String email;

	@Column(name = "pronouns")
	private String pronouns;

	@Column(name = "gender")
	private String gender;
	/*
	@Column(name = "qualification")
	private String qualification;
	
	@Column(name = "currentOccupation")
	private String currentOccupation;
	*/
	@Column(name = "modeofsession")
	private String modeOfSession;
	
	@Column(name = "preferredday")
	private int preferredDay;
	
}
