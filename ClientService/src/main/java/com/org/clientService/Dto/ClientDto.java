package com.org.clientService.Dto;

import java.sql.Date;

import com.org.events.Client.ClientStatus;

import lombok.Data;

@Data
public class ClientDto {
	
	private String clientId;
	private String firstName;
	private String lastName;
	private String fullName;
	private Date dob;
	private String phoneNumber;
	private String emergencyPhoneNumber;
	private String email;
	private String pronouns;
	private String gender;
	private String qualification;
	private String currentOccupation;
	private String preferredDays;
	private String preferredTimings;
	private String preferredModes;
	private String emergencyContactName;
	private Integer emergencyContactAge;
	private String emergencyContactRelationship;
	private java.math.BigDecimal sessionFee;
	private String source = "MANUAL";
	private String therapistId;
	private ClientStatus status;
	/* No default: absent must stay distinguishable from an explicit false, or an
	   update that simply omits the flag would un-mark a pro-bono client. Both
	   readers are null-safe (Boolean.TRUE.equals / the entity's primitive). */
	private Boolean dsf;
	//private String qualification;
	//private String currentOccupation;
}
