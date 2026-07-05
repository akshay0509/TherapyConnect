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
 * Verifies the outbox publisher's at-least-once semantics and the purge:
 * - events are marked published only after a successful send
 * - a send failure stops the batch (ordering preserved, retried next tick)
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
		when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc())
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
	void sendFailureStopsTheBatchWithoutMarkingAnything() {
		OutboxEvent first = event("OUT1", "AGG1");
		OutboxEvent second = event("OUT2", "AGG2");
		when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc())
				.thenReturn(List.of(first, second));
		doThrow(new RuntimeException("kafka down"))
				.when(outboxEventProducer).sendMessage(eq("AGG1"), any());

		scheduler.publishPendingEvents();

		// failed event stays unpublished, and the batch stops so ordering
		// is preserved — the next tick retries from the same point
		assertThat(first.isPublished()).isFalse();
		assertThat(second.isPublished()).isFalse();
		verify(outboxEventProducer, never()).sendMessage(eq("AGG2"), any());
		verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
	}

	@Test
	void emptyOutboxSendsNothing() {
		when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc())
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
