package com.org.analyticsService.Messaging.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.analyticsService.Services.AnalyticsAggregationService;
import com.org.events.TherapistAppointment.AppointmentEvent;

@Service
public class AppointmentEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentEventConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AnalyticsAggregationService analyticsAggregationService;

    @KafkaListener(topics = "therapist-appointment-events", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void listen(JsonNode payload) {
        String eventType = payload.get("eventType").asText();
        AppointmentEvent event = objectMapper.convertValue(payload, AppointmentEvent.class);

        switch (eventType) {
            case "AppointmentCompleted"    -> analyticsAggregationService.handleCompleted(event);
            case "AppointmentCancelled"    -> analyticsAggregationService.handleCancelled(event);
            case "AppointmentAbandoned"    -> analyticsAggregationService.handleAbandoned(event);
            case "AppointmentRescheduled"  -> analyticsAggregationService.handleRescheduled(event);
            default -> logger.debug("Skipping eventType={} for analytics", eventType);
        }
    }
}
