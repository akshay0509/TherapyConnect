package com.org.clientService.Services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.clientService.Assembler.ClientAssembler;
import com.org.clientService.Dto.ClientDto;
import com.org.clientService.Dto.ClientFeeHistoryDto;
import com.org.clientService.Entity.Client;
import com.org.clientService.Entity.ClientFeeHistory;
import com.org.clientService.Repository.ClientFeeHistoryRepository;
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

	@Autowired
	private ClientFeeHistoryRepository clientFeeHistoryRepository;
	
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
	
	/**
	 * A negotiated rate is either absent or a real amount. Zero is not a free
	 * session — DSF is how a client is marked pro bono — so it is stored as
	 * "no negotiated rate" and the mode price applies. A negative rate is
	 * always a mistake, and silently keeping one would be worse than failing:
	 * resolveSessionFee ignores non-positive rates, so the stored value would
	 * never apply and the therapist would see a fee they did not set.
	 */
	private BigDecimal normaliseSessionFee(BigDecimal sessionFee) {
		if (sessionFee == null) {
			return null;
		}
		if (sessionFee.signum() < 0) {
			throw new IllegalArgumentException("Session fee cannot be negative.");
		}
		if (sessionFee.signum() == 0) {
			return null;
		}
		return sessionFee.setScale(2, java.math.RoundingMode.HALF_UP);
	}

	/** Appends a row only when the rate actually moved, so an unrelated edit
	 *  does not pad the history with no-op entries. */
	private void recordFeeChange(Client client, BigDecimal oldFee, BigDecimal newFee) {
		boolean unchanged = oldFee == null
				? newFee == null
				: newFee != null && oldFee.compareTo(newFee) == 0;
		if (unchanged) {
			return;
		}
		ClientFeeHistory history = new ClientFeeHistory();
		history.setClientId(client.getClientId());
		history.setTherapistId(client.getTherapistId());
		history.setOldFee(oldFee);
		history.setNewFee(newFee);
		history.setChangedAt(LocalDateTime.now());
		clientFeeHistoryRepository.save(history);
	}

	public List<ClientFeeHistoryDto> getFeeHistory(String therapistId, String clientId) {
		List<ClientFeeHistoryDto> result = new ArrayList<>();
		for (ClientFeeHistory row : clientFeeHistoryRepository
				.findByTherapistIdAndClientIdOrderByChangedAtDesc(therapistId, clientId)) {
			ClientFeeHistoryDto dto = new ClientFeeHistoryDto();
			dto.setFeeHistoryId(row.getFeeHistoryId());
			dto.setOldFee(row.getOldFee());
			dto.setNewFee(row.getNewFee());
			dto.setChangedAt(row.getChangedAt());
			result.add(dto);
		}
		return result;
	}

	public ClientDto getClient(String therapistId, String clientId) {
		Client client = clientRepository.findByTherapistIdAndClientId(therapistId, clientId);
		ClientDto clientDto = clientAssembler.assembleEntityToDto(client);
		
		return clientDto;
	}
	
	@Transactional
	public String createClient(ClientDto clientDto) throws JsonProcessingException {
		Client client = clientAssembler.assembleDtoToEntity(clientDto);
		client.setSessionFee(normaliseSessionFee(client.getSessionFee()));
		clientRepository.save(client);
		recordFeeChange(client, null, client.getSessionFee());
		
		ClientEvent clientEvent = new ClientEvent();
		clientEvent.setEventType("ClientCreated");
		clientEvent.setClientId(client.getClientId());
		clientEvent.setTherapistId(client.getTherapistId());
		clientEvent.setEmail(client.getEmail());
		clientEvent.setPhoneNumber(client.getPhoneNumber());
		clientEvent.setFirstName(client.getFirstName());
		clientEvent.setLastName(client.getLastName());
		clientEvent.setFullName(client.getFullName());
		clientEvent.setOccurredAt(LocalDateTime.now());
		clientEvent.setStatus(client.getStatus());
		clientEvent.setDsf(client.isDsf());
		clientEvent.setSessionFee(client.getSessionFee());

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
		client.setFullName(clientDto.getFullName() == null || clientDto.getFullName().isBlank()
				? ((clientDto.getFirstName() == null ? "" : clientDto.getFirstName()) + " "
						+ (clientDto.getLastName() == null ? "" : clientDto.getLastName())).trim()
				: clientDto.getFullName().trim());
		client.setDob(clientDto.getDob());
		client.setPhoneNumber(clientDto.getPhoneNumber());
		client.setEmergencyPhoneNumber(clientDto.getEmergencyPhoneNumber());
		client.setEmail(clientDto.getEmail());
		client.setPronouns(clientDto.getPronouns());
		client.setGender(clientDto.getGender());
		client.setQualification(clientDto.getQualification());
		client.setCurrentOccupation(clientDto.getCurrentOccupation());
		client.setPreferredDays(clientDto.getPreferredDays());
		client.setPreferredTimings(clientDto.getPreferredTimings());
		client.setPreferredModes(clientDto.getPreferredModes());
		client.setEmergencyContactName(clientDto.getEmergencyContactName());
		client.setEmergencyContactAge(clientDto.getEmergencyContactAge());
		client.setEmergencyContactRelationship(clientDto.getEmergencyContactRelationship());
		/* DSF was previously fixed at creation with no way back, which left a
		   mistaken tick permanent and a finished pro-bono arrangement stuck.
		   Null means the caller is not managing the flag, so it is left alone. */
		if (clientDto.getDsf() != null) {
			client.setDsf(clientDto.getDsf());
		}

		BigDecimal previousFee = client.getSessionFee();
		BigDecimal nextFee = normaliseSessionFee(clientDto.getSessionFee());
		client.setSessionFee(nextFee);

		Client saved = clientRepository.save(client);
		recordFeeChange(saved, previousFee, nextFee);
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
		clientEvent.setFullName(client.getFullName());
		clientEvent.setOccurredAt(LocalDateTime.now());
		clientEvent.setStatus(client.getStatus());
		clientEvent.setDsf(client.isDsf());
		clientEvent.setSessionFee(client.getSessionFee());
		outboxService.saveOutboxEvent("CLIENT", client.getClientId(), eventType, clientEvent);
	}

}
