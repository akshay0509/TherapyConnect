package com.org.clientService.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.org.clientService.Entity.ClientDto;
import com.org.clientService.Services.ClientService;

@RestController
public class ClientController {

	@Autowired
	private ClientService clientService;
	
	@GetMapping("/clients")
	public List<ClientDto> getAllClients(){
		return clientService.getAllClients();
	}
	
	@PostMapping("/client")
	public void createClient(@RequestBody ClientDto clientDto) {
		clientService.createClient(clientDto);
	}
}
