package com.org.appointmentService.Messaging.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.appointmentService.Entity.TherapistServiceProjection;
import com.org.appointmentService.Repository.TherapistServiceProjectionRepository;

@ExtendWith(MockitoExtension.class)
class TherapistServiceDefinitionConsumerTest {

	@Mock
	private TherapistServiceProjectionRepository repository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private TherapistServiceDefinitionConsumer consumer;

	@BeforeEach
	void setUp() {
		consumer = new TherapistServiceDefinitionConsumer(repository, objectMapper);
	}

	@Test
	void upsertsBookingRelevantServiceDefinition() throws Exception {
		JsonNode payload = objectMapper.readTree("""
				{
				  "eventType": "TherapistServiceDefinitionUpserted",
				  "serviceId": "SRV1",
				  "therapistId": "THR1",
				  "durationMinutes": 60,
				  "isActive": true
				}
				""");
		when(repository.findById("SRV1")).thenReturn(Optional.empty());

		consumer.process(payload);

		ArgumentCaptor<TherapistServiceProjection> captor =
				ArgumentCaptor.forClass(TherapistServiceProjection.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getDurationMinutes()).isEqualTo(60);
		assertThat(captor.getValue().isActive()).isTrue();
	}

	@Test
	void deleteRemovesProjection() throws Exception {
		JsonNode payload = objectMapper.readTree("""
				{
				  "eventType": "TherapistServiceDefinitionDeleted",
				  "serviceId": "SRV1",
				  "therapistId": "THR1"
				}
				""");

		consumer.process(payload);

		verify(repository).deleteById("SRV1");
	}
}
