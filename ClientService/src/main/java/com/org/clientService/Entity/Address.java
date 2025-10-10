package com.org.clientService.Entity;

import lombok.Data;

@Data
public class Address {
	String houseName;
	String houseNumber;
	String addressLine1;
	String addressLine2;
	String areaName;
	String city;
	String state;
	String country;
	int pinCode;
}
