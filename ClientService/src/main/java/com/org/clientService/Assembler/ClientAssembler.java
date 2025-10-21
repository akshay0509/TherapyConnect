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
		client.setModeOfSession(clientDto.getModeOfSession());
		client.setPhoneNumber(clientDto.getPhoneNumber());
		client.setPreferredDay(clientDto.getPreferredDay());
		client.setPronouns(clientDto.getPronouns());
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
		clientDto.setModeOfSession(client.getModeOfSession());
		clientDto.setPhoneNumber(client.getPhoneNumber());
		clientDto.setPreferredDay(client.getPreferredDay());
		clientDto.setPronouns(client.getPronouns());
		//clientDto.setQualification(client.getQualification());
		
		return clientDto;
	}
	
}
