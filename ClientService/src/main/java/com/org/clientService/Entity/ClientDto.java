package com.org.clientService.Entity;

import java.util.Date;

import lombok.Data;

@Data
public class ClientDto {
	
	public enum ModesOfSession{
		ONLINE("O"),
		OFFLINE_AT_HALUSURU("H"),
		OFFLINE_AT_SESHADRIPURAM("S");
		
		private final String code;

		ModesOfSession(String code) {
	        this.code = code;
	    }

	    public String getCode() {
	        return this.code;
	    }

	    // Custom lookup method
	    public static ModesOfSession fromCode(String code) {
	        for (ModesOfSession status : ModesOfSession.values()) {
	            if (status.getCode().equals(code)) {
	                return status;
	            }
	        }
	        // Handle the case where the code is not found
	        throw new IllegalArgumentException("Unknown status code: " + code);
	    }
	}
	
	public enum Day {
	    MONDAY(1),
	    TUESDAY(2),
	    WEDNESDAY(3),
	    THURSDAY(4),
	    FRIDAY(5),
	    SATURDAY(6),
	    SUNDAY(7);
		
		private final int code;

		Day(int code) {
	        this.code = code;
	    }

	    public int getCode() {
	        return this.code;
	    }

	    // Custom lookup method
	    public static Day fromCode(int code) {
	        for (Day status : Day.values()) {
	            if (status.getCode() == code) {
	                return status;
	            }
	        }
	        // Handle the case where the code is not found
	        throw new IllegalArgumentException("Unknown status code: " + code);
	    }
	}

	private String firstName;
	private String lastName;
	private Date dob;
	private String phoneNumber;
	private String emergencyPhoneNumber;
	private String email;
	private String pronouns;
	private String gender;
	//private String qualification;
	//private String currentOccupation;
	private String modeOfSession;
	private int preferredDay;
}
