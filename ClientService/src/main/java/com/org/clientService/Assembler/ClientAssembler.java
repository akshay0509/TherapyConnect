package com.org.clientService.Assembler;

import java.time.LocalDate;
import java.time.Period;

import com.org.clientService.Dto.ClientDto;
import com.org.clientService.Entity.Client;

public class ClientAssembler {

	public Client assembleDtoToEntity(ClientDto clientDto) {
		Client client = new Client();
		String source = clientDto.getSource() == null ? "MANUAL" : clientDto.getSource();
		client.setAge(clientDto.getDob() == null ? 0
				: Period.between(clientDto.getDob().toLocalDate(), LocalDate.now()).getYears());
		client.setCurrentOccupation(clientDto.getCurrentOccupation());
		client.setDob(clientDto.getDob());
		client.setEmail(clientDto.getEmail());
		client.setEmergencyPhoneNumber(clientDto.getEmergencyPhoneNumber());
		client.setFirstName(clientDto.getFirstName());
		client.setLastName(clientDto.getLastName());
		client.setFullName("GOOGLE_FORM".equals(source)
				? resolveFullName(clientDto.getFullName(), clientDto.getFirstName(), clientDto.getLastName())
				: resolveFullName(null, clientDto.getFirstName(), clientDto.getLastName()));
		client.setGender(clientDto.getGender());
		client.setPhoneNumber(clientDto.getPhoneNumber());
		client.setPronouns(clientDto.getPronouns());
		client.setTherapistId(clientDto.getTherapistId());
		client.setDsf(Boolean.TRUE.equals(clientDto.getDsf()));
		client.setQualification(clientDto.getQualification());
		client.setPreferredDays(clientDto.getPreferredDays());
		client.setPreferredTimings(clientDto.getPreferredTimings());
		client.setPreferredModes(clientDto.getPreferredModes());
		client.setEmergencyContactName(clientDto.getEmergencyContactName());
		client.setEmergencyContactAge(clientDto.getEmergencyContactAge());
		client.setEmergencyContactRelationship(clientDto.getEmergencyContactRelationship());
		client.setSource(source);
		
		return client;
	}
	
	public ClientDto assembleEntityToDto(Client client) {
		ClientDto clientDto = new ClientDto();
		clientDto.setCurrentOccupation(client.getCurrentOccupation());
		clientDto.setClientId(client.getClientId());
		clientDto.setDob(client.getDob());
		clientDto.setEmail(client.getEmail());
		clientDto.setEmergencyPhoneNumber(client.getEmergencyPhoneNumber());
		clientDto.setFirstName(client.getFirstName());
		clientDto.setLastName(client.getLastName());
		clientDto.setFullName(resolveFullName(client.getFullName(), client.getFirstName(), client.getLastName()));
		clientDto.setGender(client.getGender());
		clientDto.setPhoneNumber(client.getPhoneNumber());
		clientDto.setPronouns(client.getPronouns());
		clientDto.setStatus(client.getStatus());
		clientDto.setDsf(client.isDsf());
		clientDto.setQualification(client.getQualification());
		clientDto.setPreferredDays(client.getPreferredDays());
		clientDto.setPreferredTimings(client.getPreferredTimings());
		clientDto.setPreferredModes(client.getPreferredModes());
		clientDto.setEmergencyContactName(client.getEmergencyContactName());
		clientDto.setEmergencyContactAge(client.getEmergencyContactAge());
		clientDto.setEmergencyContactRelationship(client.getEmergencyContactRelationship());
		clientDto.setSource(client.getSource());
		
		return clientDto;
	}

	private String resolveFullName(String fullName, String firstName, String lastName) {
		if (fullName != null && !fullName.isBlank()) {
			return fullName.trim();
		}
		return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
	}
	
}
