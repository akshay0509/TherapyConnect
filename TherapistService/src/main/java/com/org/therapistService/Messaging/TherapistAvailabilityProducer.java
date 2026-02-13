package com.org.therapistService.Messaging;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.org.therapistService.Entity.OutboxEvent;
import com.org.therapistService.Repository.OutboxEventRepository;

import jakarta.transaction.Transactional;

@Service
public class TherapistAvailabilityProducer {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	private static final String topic = "therapist-availability-events";

	@Scheduled(fixedDelay = 2000)
	@Transactional
	public void publishPendingEvents() {

		List<OutboxEvent> outboxEventList = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();

		for(OutboxEvent outboxEvent : outboxEventList) {

			try {
				kafkaTemplate.send(topic, outboxEvent.getAggregateId(), outboxEvent.getPayload()).get(); // block for ACK

				outboxEvent.setPublished(true);

			}
			catch (Exception ex) {
				break;
			}
		}
	}
}
