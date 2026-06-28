package com.org.analyticsService.Messaging.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.analyticsService.Entity.ClientDsfProjection;
import com.org.analyticsService.Entity.ClientDsfProjectionId;
import com.org.analyticsService.Repository.ClientDsfProjectionRepository;
import com.org.events.Client.ClientEvent;

@Service
public class ClientEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ClientEventConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientDsfProjectionRepository clientDsfProjectionRepository;

    @KafkaListener(topics = "client-events", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void process(JsonNode payload) {
        String eventType = payload.get("eventType").asText();
        ClientEvent event = objectMapper.convertValue(payload, ClientEvent.class);

        switch (eventType) {
            case "ClientCreated", "ClientUpdated" -> upsertDsfProjection(event);
            default -> logger.debug("Skipping unsupported client eventType={}", eventType);
        }
    }

    private void upsertDsfProjection(ClientEvent event) {
        ClientDsfProjectionId id = new ClientDsfProjectionId(event.getClientId(), event.getTherapistId());
        ClientDsfProjection projection = clientDsfProjectionRepository.findById(id)
                .orElseGet(() -> {
                    ClientDsfProjection p = new ClientDsfProjection();
                    p.setClientId(event.getClientId());
                    p.setTherapistId(event.getTherapistId());
                    return p;
                });
        projection.setDsf(event.isDsf());
        clientDsfProjectionRepository.save(projection);
    }
}
