package com.org.appointmentService.Scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.kafka.common.errors.RecordTooLargeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.appointmentService.Entity.OutboxEvent;
import com.org.appointmentService.Messaging.Producer.OutboxEventProducer;
import com.org.appointmentService.Repository.OutboxEventRepository;

/**
 * Verifies the outbox publisher's at-least-once semantics, the poison-event
 * park-and-skip guard, and the purge:
 * - events are marked published only after a successful send
 * - a transient send failure stops the batch (ordering preserved, retried
 *   next tick) and counts a retry
 * - a fatal failure (e.g. record too large — can never succeed) parks the
 *   event and the batch continues past it
 * - exhausting the retry budget parks the event
 * - the purge removes only published rows past the 7-day retention
 */
@ExtendWith(MockitoExtension.class)
class OutboxEventSchedulerTest {

	@Mock
	private OutboxEventRepository outboxEventRepository;

	@Mock
	private OutboxEventProducer outboxEventProducer;

	@InjectMocks
	private OutboxEventScheduler scheduler;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private OutboxEvent event(String id, String aggregateId) {
		OutboxEvent event = new OutboxEvent();
		event.setOutboxEventId(id);
		event.setAggregateType("THERAPIST_APPOINTMENT");
		event.setAggregateId(aggregateId);
		event.setEventType("AppointmentCreated");
		event.setPayload(MAPPER.createObjectNode().put("eventType", "AppointmentCreated"));
		event.setCreatedAt(LocalDateTime.now());
		event.setPublished(false);
		return event;
	}

	@Test
	void publishedEventsAreMarkedAndSaved() {
		OutboxEvent first = event("OUT1", "AGG1");
		OutboxEvent second = event("OUT2", "AGG2");
		when(outboxEventRepository.findTop100ByPublishedFalseAndParkedFalseOrderByCreatedAtAsc())
				.thenReturn(List.of(first, second));

		scheduler.publishPendingEvents();

		verify(outboxEventProducer).sendMessage(eq("AGG1"), any());
		verify(outboxEventProducer).sendMessage(eq("AGG2"), any());
		assertThat(first.isPublished()).isTrue();
		assertThat(second.isPublished()).isTrue();
		verify(outboxEventRepository).save(first);
		verify(outboxEventRepository).save(second);
	}

	@Test
	void transientFailureStopsTheBatchAndCountsARetry() {
		OutboxEvent first = event("OUT1", "AGG1");
		OutboxEvent second = event("OUT2", "AGG2");
		when(outboxEventRepository.findTop100ByPublishedFalseAndParkedFalseOrderByCreatedAtAsc())
				.thenReturn(List.of(first, second));
		doThrow(new RuntimeException("kafka down"))
				.when(outboxEventProducer).sendMessage(eq("AGG1"), any());

		scheduler.publishPendingEvents();

		// failed event stays unpublished with the attempt recorded, and the
		// batch stops so ordering is preserved — the next tick retries
		assertThat(first.isPublished()).isFalse();
		assertThat(first.isParked()).isFalse();
		assertThat(first.getRetryCount()).isEqualTo(1);
		assertThat(second.isPublished()).isFalse();
		verify(outboxEventProducer, never()).sendMessage(eq("AGG2"), any());
		verify(outboxEventRepository).save(first);
		verify(outboxEventRepository, never()).save(second);
	}

	@Test
	void fatalFailureParksTheEventAndTheBatchContinues() {
		OutboxEvent poison = event("OUT1", "AGG1");
		OutboxEvent healthy = event("OUT2", "AGG2");
		when(outboxEventRepository.findTop100ByPublishedFalseAndParkedFalseOrderByCreatedAtAsc())
				.thenReturn(List.of(poison, healthy));
		doThrow(new RuntimeException("Kafka publish failed", new RecordTooLargeException()))
				.when(outboxEventProducer).sendMessage(eq("AGG1"), any());

		scheduler.publishPendingEvents();

		assertThat(poison.isParked()).isTrue();
		assertThat(poison.isPublished()).isFalse();
		// the event behind the poison one is no longer blocked
		assertThat(healthy.isPublished()).isTrue();
		verify(outboxEventRepository).save(poison);
		verify(outboxEventRepository).save(healthy);
	}

	@Test
	void exhaustedRetryBudgetParksTheEvent() {
		OutboxEvent stuck = event("OUT1", "AGG1");
		stuck.setRetryCount(149); // one attempt away from the 150 ceiling
		when(outboxEventRepository.findTop100ByPublishedFalseAndParkedFalseOrderByCreatedAtAsc())
				.thenReturn(List.of(stuck));
		doThrow(new RuntimeException("kafka down"))
				.when(outboxEventProducer).sendMessage(eq("AGG1"), any());

		scheduler.publishPendingEvents();

		assertThat(stuck.getRetryCount()).isEqualTo(150);
		assertThat(stuck.isParked()).isTrue();
	}

	@Test
	void emptyOutboxSendsNothing() {
		when(outboxEventRepository.findTop100ByPublishedFalseAndParkedFalseOrderByCreatedAtAsc())
				.thenReturn(List.of());

		scheduler.publishPendingEvents();

		verifyNoInteractions(outboxEventProducer);
	}

	@Test
	void purgeDeletesPublishedRowsPastSevenDayRetention() {
		when(outboxEventRepository.deleteByPublishedTrueAndCreatedAtBefore(any())).thenReturn(30L);

		scheduler.purgePublishedEvents();

		ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(outboxEventRepository).deleteByPublishedTrueAndCreatedAtBefore(cutoff.capture());

		LocalDateTime now = LocalDateTime.now();
		assertThat(cutoff.getValue()).isAfter(now.minusDays(8));
		assertThat(cutoff.getValue()).isBefore(now.minusDays(6));
	}
}
