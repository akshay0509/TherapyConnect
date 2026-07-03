package com.org.clientService.Services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.clientService.Assembler.ClientAssembler;
import com.org.clientService.Dto.ClientDto;
import com.org.clientService.Entity.Client;
import com.org.clientService.Repository.ClientRepository;
import com.org.events.Client.ClientEvent;
import com.org.events.Client.ClientStatus;

import jakarta.transaction.Transactional;

@Service
public class ClientService {

	@Autowired
	private ClientRepository clientRepository;
	
	@Autowired
	private OutboxService outboxService;
	
	private ClientAssembler clientAssembler = new ClientAssembler();
	
	public List<ClientDto> getAllClients(){
		List<Client> clientList = clientRepository.findAll();
		List<ClientDto> clientDtoList = new ArrayList<ClientDto>();
		ClientDto clientDto;
		for(Client client : clientList) {
			clientDto = clientAssembler.assembleEntityToDto(client);
			clientDtoList.add(clientDto);
		}
		return clientDtoList;
	}
	
	public ClientDto getClient(String therapistId, String clientId) {
		Client client = clientRepository.findByTherapistIdAndClientId(therapistId, clientId);
		ClientDto clientDto = clientAssembler.assembleEntityToDto(client);
		
		return clientDto;
	}
	
	@Transactional
	public String createClient(ClientDto clientDto) throws JsonProcessingException {
		Client client = clientAssembler.assembleDtoToEntity(clientDto);
		clientRepository.save(client);
		
		ClientEvent clientEvent = new ClientEvent();
		clientEvent.setEventType("ClientCreated");
		clientEvent.setClientId(client.getClientId());
		clientEvent.setTherapistId(client.getTherapistId());
		clientEvent.setEmail(client.getEmail());
		clientEvent.setPhoneNumber(client.getPhoneNumber());
		clientEvent.setFirstName(client.getFirstName());
		clientEvent.setLastName(client.getLastName());
		clientEvent.setOccurredAt(LocalDateTime.now());
		clientEvent.setStatus(client.getStatus());
		clientEvent.setDsf(client.isDsf());

		outboxService.saveOutboxEvent("CLIENT", client.getClientId(), "ClientCreated", clientEvent);
		
		return client.getClientId();
	}
	
	@Transactional
	public ClientDto updateClient(String therapistId, String clientId, ClientDto clientDto) throws JsonProcessingException {
		Client client = clientRepository.findByTherapistIdAndClientId(therapistId, clientId);
		if (client == null) {
			throw new IllegalArgumentException("Client not found.");
		}

		client.setFirstName(clientDto.getFirstName());
		client.setLastName(clientDto.getLastName());
		client.setDob(clientDto.getDob());
		client.setPhoneNumber(clientDto.getPhoneNumber());
		client.setEmergencyPhoneNumber(clientDto.getEmergencyPhoneNumber());
		client.setEmail(clientDto.getEmail());
		client.setPronouns(clientDto.getPronouns());
		client.setGender(clientDto.getGender());

		Client saved = clientRepository.save(client);
		publishClientEvent("ClientUpdated", saved);
		return clientAssembler.assembleEntityToDto(saved);
	}
	
	@Transactional
	public ClientDto updateClientStatus(String therapistId, String clientId, ClientStatus status) throws JsonProcessingException {
		if (status == null) {
			throw new IllegalArgumentException("Client status is required.");
		}

		Client client = clientRepository.findByTherapistIdAndClientId(therapistId, clientId);
		if (client == null) {
			throw new IllegalArgumentException("Client not found.");
		}

		client.setStatus(status);
		Client saved = clientRepository.save(client);
		publishClientEvent("ClientStatusUpdated", saved);
		return clientAssembler.assembleEntityToDto(saved);
	}
	
	

	private void publishClientEvent(String eventType, Client client) throws JsonProcessingException {
		ClientEvent clientEvent = new ClientEvent();
		clientEvent.setEventType(eventType);
		clientEvent.setClientId(client.getClientId());
		clientEvent.setTherapistId(client.getTherapistId());
		clientEvent.setEmail(client.getEmail());
		clientEvent.setPhoneNumber(client.getPhoneNumber());
		clientEvent.setFirstName(client.getFirstName());
		clientEvent.setLastName(client.getLastName());
		clientEvent.setOccurredAt(LocalDateTime.now());
		clientEvent.setStatus(client.getStatus());
		clientEvent.setDsf(client.isDsf());
		outboxService.saveOutboxEvent("CLIENT", client.getClientId(), eventType, clientEvent);
	}

}
