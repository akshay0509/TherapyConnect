package com.org.clientService.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.org.clientService.Assembler.ClientAssembler;
import com.org.clientService.Dto.ClientDto;
import com.org.clientService.Entity.Client;
import com.org.clientService.Entity.ClientFeeHistory;
import com.org.clientService.Repository.ClientFeeHistoryRepository;
import com.org.clientService.Repository.ClientRepository;
import com.org.events.Client.ClientStatus;

/**
 * updateClient is a full replace: it assigns every field it knows about
 * unconditionally, so anything ClientDto cannot carry is written as null. Adding
 * a column to Client and forgetting it in ClientDto therefore wipes that column
 * on the next edit — silently, with no error anywhere.
 *
 * This test closes that hole. It fills every field on Client by reflection, sends
 * the entity out through the assembler and straight back in through updateClient,
 * and asserts nothing changed. A new column that does not survive the trip fails
 * the build and names itself in the failure message.
 */
@ExtendWith(MockitoExtension.class)
class ClientUpdateRoundTripTest {

	private static final String THERAPIST_ID = "THP123";
	private static final String CLIENT_ID = "CLI456";

	@Mock private ClientRepository clientRepository;
	@Mock private ClientFeeHistoryRepository clientFeeHistoryRepository;
	@Mock private OutboxService outboxService;

	@InjectMocks private ClientService clientService;

	/**
	 * Fields updateClient deliberately does not take from the request. They are
	 * excluded here rather than ignored so that adding to this list is a visible
	 * decision — every other field is checked automatically.
	 */
	private static final java.util.Set<String> NOT_CLIENT_SUPPLIED = java.util.Set.of(
			"clientId",     // path variable
			"therapistId",  // from the JWT, never the body
			"createdAt",    // set once at creation
			"status",       // owned by updateClientStatus
			"age",          // derived
			"source");      // provenance of the record

	@Test
	void everyClientFieldSurvivesAnEditThatChangesNothing() throws Exception {
		Client stored = fullyPopulatedClient();
		Map<String, Object> before = snapshot(stored);

		when(clientRepository.findByTherapistIdAndClientId(THERAPIST_ID, CLIENT_ID)).thenReturn(stored);
		when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

		// The exact payload the UI gets back from a read, sent straight back.
		ClientDto asRead = new ClientAssembler().assembleEntityToDto(stored);

		clientService.updateClient(THERAPIST_ID, CLIENT_ID, asRead);

		ArgumentCaptor<Client> savedCaptor = ArgumentCaptor.forClass(Client.class);
		org.mockito.Mockito.verify(clientRepository).save(savedCaptor.capture());
		Map<String, Object> after = snapshot(savedCaptor.getValue());

		for (Map.Entry<String, Object> entry : before.entrySet()) {
			assertThat(after.get(entry.getKey()))
					.as("Client.%s did not survive a read-then-write round trip - "
							+ "it is probably missing from ClientDto or the assembler", entry.getKey())
					.isEqualTo(entry.getValue());
		}
	}

	@Test
	void negativeSessionFeeIsRejected() {
		Client stored = new Client();
		stored.setClientId(CLIENT_ID);
		stored.setTherapistId(THERAPIST_ID);
		when(clientRepository.findByTherapistIdAndClientId(THERAPIST_ID, CLIENT_ID)).thenReturn(stored);

		ClientDto dto = new ClientDto();
		dto.setSessionFee(new BigDecimal("-1"));

		assertThat(org.junit.jupiter.api.Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> clientService.updateClient(THERAPIST_ID, CLIENT_ID, dto)))
				.hasMessageContaining("negative");
	}

	@Test
	void zeroSessionFeeMeansNoNegotiatedRate() throws Exception {
		Client stored = new Client();
		stored.setClientId(CLIENT_ID);
		stored.setTherapistId(THERAPIST_ID);
		stored.setSessionFee(new BigDecimal("1200.00"));
		when(clientRepository.findByTherapistIdAndClientId(THERAPIST_ID, CLIENT_ID)).thenReturn(stored);
		when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

		ClientDto dto = new ClientDto();
		dto.setSessionFee(BigDecimal.ZERO);

		// Zero is not a free session — DSF is how that is expressed — so it
		// clears the rate and the mode price applies again.
		assertThat(clientService.updateClient(THERAPIST_ID, CLIENT_ID, dto).getSessionFee()).isNull();
	}

	@Test
	void omittingDsfLeavesAProBonoClientMarked() throws Exception {
		Client stored = new Client();
		stored.setClientId(CLIENT_ID);
		stored.setTherapistId(THERAPIST_ID);
		stored.setDsf(true);
		when(clientRepository.findByTherapistIdAndClientId(THERAPIST_ID, CLIENT_ID)).thenReturn(stored);
		when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

		ClientDto dto = new ClientDto();   // dsf absent, not false
		dto.setFirstName("Asha");

		assertThat(clientService.updateClient(THERAPIST_ID, CLIENT_ID, dto).getDsf()).isTrue();
	}

	@Test
	void dsfCanBeClearedWhenTheArrangementEnds() throws Exception {
		Client stored = new Client();
		stored.setClientId(CLIENT_ID);
		stored.setTherapistId(THERAPIST_ID);
		stored.setDsf(true);
		when(clientRepository.findByTherapistIdAndClientId(THERAPIST_ID, CLIENT_ID)).thenReturn(stored);
		when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

		ClientDto dto = new ClientDto();
		dto.setDsf(false);

		assertThat(clientService.updateClient(THERAPIST_ID, CLIENT_ID, dto).getDsf()).isFalse();
	}

	@Test
	void aRateChangeIsRecordedButAnUnrelatedEditIsNot() throws Exception {
		Client stored = new Client();
		stored.setClientId(CLIENT_ID);
		stored.setTherapistId(THERAPIST_ID);
		stored.setSessionFee(new BigDecimal("1500.00"));
		when(clientRepository.findByTherapistIdAndClientId(THERAPIST_ID, CLIENT_ID)).thenReturn(stored);
		when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

		ClientDto sameFee = new ClientDto();
		sameFee.setSessionFee(new BigDecimal("1500.00"));
		clientService.updateClient(THERAPIST_ID, CLIENT_ID, sameFee);
		org.mockito.Mockito.verify(clientFeeHistoryRepository, org.mockito.Mockito.never())
				.save(any(ClientFeeHistory.class));

		ClientDto newFee = new ClientDto();
		newFee.setSessionFee(new BigDecimal("1200"));
		clientService.updateClient(THERAPIST_ID, CLIENT_ID, newFee);

		ArgumentCaptor<ClientFeeHistory> captor = ArgumentCaptor.forClass(ClientFeeHistory.class);
		org.mockito.Mockito.verify(clientFeeHistoryRepository).save(captor.capture());
		assertThat(captor.getValue().getOldFee()).isEqualByComparingTo("1500.00");
		assertThat(captor.getValue().getNewFee()).isEqualByComparingTo("1200");
	}

	// ── helpers ─────────────────────────────────────────────────────────────

	/** Distinct non-null value per field, so a dropped field shows as a change
	 *  rather than coincidentally matching a default. */
	private Client fullyPopulatedClient() throws Exception {
		Client client = new Client();
		int seed = 1;
		for (Field field : mutableFields()) {
			field.setAccessible(true);
			field.set(client, sampleValue(field.getType(), seed++));
		}
		client.setClientId(CLIENT_ID);
		client.setTherapistId(THERAPIST_ID);
		// updateClient recomputes fullName from the parts when the DTO's is
		// blank, so it has to be consistent for a no-change edit to be a no-op.
		client.setFullName((client.getFirstName() + " " + client.getLastName()).trim());
		return client;
	}

	private Object sampleValue(Class<?> type, int seed) {
		if (type == String.class) return "value" + seed;
		if (type == Integer.class || type == int.class) return seed;
		if (type == Boolean.class || type == boolean.class) return true;
		if (type == BigDecimal.class) return new BigDecimal(seed + "00.00");
		if (type == java.sql.Date.class) return java.sql.Date.valueOf("1990-0" + ((seed % 9) + 1) + "-15");
		if (type == LocalDateTime.class) return LocalDateTime.of(2026, 1, 1, 0, 0).plusDays(seed);
		if (type == ClientStatus.class) return ClientStatus.ACTIVE;
		throw new IllegalStateException("ClientUpdateRoundTripTest has no sample value for "
				+ type.getName() + " — add one so the new field is actually covered");
	}

	private Map<String, Object> snapshot(Client client) throws Exception {
		Map<String, Object> values = new LinkedHashMap<>();
		for (Field field : mutableFields()) {
			if (NOT_CLIENT_SUPPLIED.contains(field.getName())) continue;
			field.setAccessible(true);
			values.put(field.getName(), field.get(client));
		}
		return values;
	}

	private java.util.List<Field> mutableFields() {
		java.util.List<Field> fields = new java.util.ArrayList<>();
		for (Field field : Client.class.getDeclaredFields()) {
			if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) continue;
			fields.add(field);
		}
		return fields;
	}
}
