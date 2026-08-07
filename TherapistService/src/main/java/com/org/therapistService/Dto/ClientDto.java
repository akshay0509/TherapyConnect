package com.org.therapistService.Dto;

import java.sql.Date;

import lombok.Data;

/**
 * The payload TherapistController accepts on /create-client and forwards to
 * ClientService via ClientServiceProxy.
 *
 * This is a *different* class from ClientService's own ClientDto, and the proxy
 * serializes this one — so any field missing here is silently dropped by Jackson
 * on the way in and never reaches ClientService, however complete the receiving
 * DTO is. Keep the two in step.
 */
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
	private Boolean dsf = false;

	private String qualification;
	private String currentOccupation;
	private String preferredDays;
	private String preferredTimings;
	private String preferredModes;
	private String emergencyContactName;
	private Integer emergencyContactAge;
	private String emergencyContactRelationship;
	private java.math.BigDecimal sessionFee;
}
