package com.org.therapistService.Scheduler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.org.therapistService.Entity.OutboxEvent;
import com.org.therapistService.Messaging.OutboxEventProducer;
import com.org.therapistService.Repository.OutboxEventRepository;

import jakarta.transaction.Transactional;

@Service
public class OutboxEventScheduler {

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Autowired
	private OutboxEventProducer outboxEventProducer;

	private static final Logger logger = LoggerFactory.getLogger(OutboxEventScheduler.class);

	@Scheduled(fixedDelay = 2000)
	@Transactional
	public void publishPendingEvents() {

		List<OutboxEvent> outboxEventList = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();

		if (outboxEventList.isEmpty()) {
			return;
		}

		logger.info("Publishing {} pending outbox events", outboxEventList.size());

		for (OutboxEvent outboxEvent : outboxEventList) {
			try {
				outboxEventProducer.sendMessage(outboxEvent.getAggregateId(), outboxEvent.getPayload());
				outboxEvent.setPublished(true);
				outboxEventRepository.save(outboxEvent);
			} catch (Exception ex) {
				logger.error("Failed to publish outbox event {}", outboxEvent.getOutboxEventId(), ex);
				break;
			}
		}
	}
}
