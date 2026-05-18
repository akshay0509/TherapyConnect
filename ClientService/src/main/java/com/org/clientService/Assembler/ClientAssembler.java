package com.org.clientService.Assembler;

import com.org.clientService.Entity.Client;
import com.org.clientService.Entity.ClientDto;

public class ClientAssembler {

	public Client assembleDtoToEntity(ClientDto clientDto) {
		Client client = new Client();
		client.setAge(12);
		//client.setCurrentOccupation(clientDto.getCurrentOccupation());
		client.setDob(clientDto.getDob());
		client.setEmail(clientDto.getEmail());
		client.setEmergencyPhoneNumber(clientDto.getEmergencyPhoneNumber());
		client.setFirstName(clientDto.getFirstName());
		client.setLastName(clientDto.getLastName());
		client.setGender(clientDto.getGender());
		client.setPhoneNumber(clientDto.getPhoneNumber());
		client.setPronouns(clientDto.getPronouns());
		client.setTherapistId(clientDto.getTherapistId());
		client.setDsf(Boolean.TRUE.equals(clientDto.getDsf()));
		//client.setQualification(clientDto.getQualification());
		
		return client;
	}
	
	public ClientDto assembleEntityToDto(Client client) {
		ClientDto clientDto = new ClientDto();
		//clientDto.setCurrentOccupation(client.getCurrentOccupation());
		clientDto.setDob(client.getDob());
		clientDto.setEmail(client.getEmail());
		clientDto.setEmergencyPhoneNumber(client.getEmergencyPhoneNumber());
		clientDto.setFirstName(client.getFirstName());
		clientDto.setLastName(client.getLastName());
		clientDto.setGender(client.getGender());
		clientDto.setPhoneNumber(client.getPhoneNumber());
		clientDto.setPronouns(client.getPronouns());
		clientDto.setStatus(client.getStatus());
		clientDto.setDsf(client.isDsf());
		
		return clientDto;
	}
	
}
