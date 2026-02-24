package com.org.clientService.Services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.clientService.Assembler.ClientAssembler;
import com.org.clientService.Entity.Client;
import com.org.clientService.Entity.ClientDto;
import com.org.clientService.Repository.ClientRepository;
import com.org.events.Client.ClientEvent;

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
		clientEvent.setEmail(client.getEmail());
		clientEvent.setFirstName(client.getFirstName());
		clientEvent.setLastName(client.getLastName());
		clientEvent.setOccurredAt(LocalDateTime.now());
		
		outboxService.saveOutboxEvent("CLIENT", client.getClientId(), "ClientCreated", clientEvent);
		
		return client.getClientId();
	}
}
