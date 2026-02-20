package com.org.clientService.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.org.clientService.Assembler.ClientAssembler;
import com.org.clientService.Entity.Client;
import com.org.clientService.Entity.ClientDto;
import com.org.clientService.Repository.ClientRepository;

@Service
public class ClientService {

	@Autowired
	private ClientRepository clientRepository;
	
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
	
	public String createClient(ClientDto clientDto) {
		Client client = clientAssembler.assembleDtoToEntity(clientDto);
		clientRepository.save(client);
		
		return client.getClientId();
	}
}
