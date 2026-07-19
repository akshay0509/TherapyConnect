package com.org.clientService.Scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.SerializationException;
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

	// ~5 minutes of 2s ticks: long enough that a broker restart never parks a
	// healthy event, short enough that a poison event can't block for long
	private static final int MAX_RETRIES = 150;

	private static final Logger logger = LoggerFactory.getLogger(OutboxEventScheduler.class);

	@Scheduled(fixedDelay = 2000)
	@Transactional
	public void publishPendingEvents() {

		List<OutboxEvent> outboxEventList = outboxEventRepository.findTop100ByPublishedFalseAndParkedFalseOrderByCreatedAtAsc();

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
				outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
				// A poison event (oversized, unserializable) can never publish
				// and would block everything behind it forever. Park it and
				// continue; parked events must only be replayed after manual
				// review — replaying them after newer events for the same
				// entity can regress projections.
				if (isFatal(ex) || outboxEvent.getRetryCount() >= MAX_RETRIES) {
					outboxEvent.setParked(true);
					outboxEventRepository.save(outboxEvent);
					logger.warn("Parked poison outbox event {} (type={}, retries={}) — manual review required",
							outboxEvent.getOutboxEventId(), outboxEvent.getEventType(), outboxEvent.getRetryCount(), ex);
					continue;
				}
				// transient (broker down/slow): keep ordering, retry next tick
				outboxEventRepository.save(outboxEvent);
				logger.error("Failed to publish outbox event {} (attempt {}) — will retry",
						outboxEvent.getOutboxEventId(), outboxEvent.getRetryCount(), ex);
				break;
			}
		}
	}

	private boolean isFatal(Throwable ex) {
		for (Throwable t = ex; t != null; t = t.getCause()) {
			if (t instanceof RecordTooLargeException || t instanceof SerializationException) {
				return true;
			}
			if (t.getCause() == t) {
				break;
			}
		}
		return false;
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
