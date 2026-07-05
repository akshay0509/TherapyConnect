package com.org.clientService.Scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.org.clientService.Entity.OutboxEvent;
import com.org.clientService.Messaging.Producer.OutboxEventProducer;
import com.org.clientService.Repository.OutboxEventRepository;

import jakarta.transaction.Transactional;

@Service
public class OutboxEventScheduler {

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Autowired
	private OutboxEventProducer outboxEventProducer;

	private static final int PURGE_RETENTION_DAYS = 7;

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

	// Published rows are never read again; without a purge the poller's
	// 2-second scan degrades as the table grows unbounded.
	@Scheduled(cron = "0 30 3 * * *")
	@Transactional
	public void purgePublishedEvents() {
		long deleted = outboxEventRepository.deleteByPublishedTrueAndCreatedAtBefore(
				LocalDateTime.now().minusDays(PURGE_RETENTION_DAYS));
		if (deleted > 0) {
			logger.info("Purged {} published outbox events older than {} days", deleted, PURGE_RETENTION_DAYS);
		}
	}
}
