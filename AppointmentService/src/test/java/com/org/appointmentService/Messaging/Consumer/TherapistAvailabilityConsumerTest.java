package com.org.appointmentService.Messaging.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.appointmentService.Entity.TherapistAvailability;
import com.org.appointmentService.Enums.AvailabilityStatus;
import com.org.appointmentService.Repository.TherapistAvailabilityRepository;
import com.org.events.TherapistAvailability.AvailabilityEvent;
import com.org.events.TherapistAvailability.AvailabilitySlotsDeletedEvent;
import com.org.events.TherapistAvailability.AvailabilitySlotsGeneratedEvent;
import com.org.events.TherapistAvailability.Slot;

/**
 * Verifies the converging projection sync:
 * - regeneration events refresh AVAILABLE slots and insert incoming ones
 * - BOOKED slots are never deleted and never double-offered — incoming slots
 *   that overlap a BOOKED time are skipped instead of rejecting the event
 *   (the old behavior parked the event in the DLT and left the projection
 *   permanently stale for that day)
 * - single-slot events respect the same invariants
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TherapistAvailabilityConsumerTest {

	@Mock
	private TherapistAvailabilityRepository therapistAvailabilityRepository;

	@Spy
	private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@InjectMocks
	private TherapistAvailabilityConsumer consumer;

	private static final String THERAPIST_ID = "THR12345";
	private static final LocalDate DAY = LocalDate.of(2026, 7, 13);

	private Slot slot(String slotId, LocalDateTime start, LocalDateTime end) {
		Slot slot = new Slot();
		slot.setSlotId(slotId);
		slot.setServiceId("SRV1");
		slot.setSessionFee(new BigDecimal("1500"));
		slot.setStartTime(start);
		slot.setEndTime(end);
		return slot;
	}

	private TherapistAvailability bookedSlot(LocalDateTime start, LocalDateTime end) {
		TherapistAvailability booked = new TherapistAvailability();
		booked.setSlotId("SLT-BOOKED");
		booked.setTherapistId(THERAPIST_ID);
		booked.setStartTime(start);
		booked.setEndTime(end);
		booked.setStatus(AvailabilityStatus.BOOKED);
		return booked;
	}

	private JsonNode generatedPayload(Slot... slots) {
		AvailabilitySlotsGeneratedEvent event = new AvailabilitySlotsGeneratedEvent();
		event.setTherapistId(THERAPIST_ID);
		event.setRangeStart(DAY);
		event.setRangeEnd(DAY);
		event.setSlotList(List.of(slots));
		return objectMapper.valueToTree(event);
	}

	private void stubBookedSlotsInRange(List<TherapistAvailability> booked) {
		when(therapistAvailabilityRepository.findByTherapistIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
				anyString(), eq(AvailabilityStatus.BOOKED), any(), any())).thenReturn(booked);
	}

	// ── batch generation ─────────────────────────────────────────────────────

	@Test
	void generationClearsAvailableSlotsAndInsertsIncoming() {
		stubBookedSlotsInRange(List.of());
		when(therapistAvailabilityRepository.existsBySlotId(anyString())).thenReturn(false);

		consumer.process(generatedPayload(
				slot("SLT1", DAY.atTime(9, 0), DAY.atTime(10, 0)),
				slot("SLT2", DAY.atTime(10, 0), DAY.atTime(11, 0))));

		verify(therapistAvailabilityRepository).deleteAvailableSlotsInRange(
				THERAPIST_ID, DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<TherapistAvailability>> captor = ArgumentCaptor.forClass(List.class);
		verify(therapistAvailabilityRepository).saveAll(captor.capture());

		assertThat(captor.getValue()).hasSize(2);
		assertThat(captor.getValue()).allSatisfy(saved -> {
			assertThat(saved.getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
			assertThat(saved.getTherapistId()).isEqualTo(THERAPIST_ID);
		});
	}

	@Test
	void generationWithBookedSlotConvergesInsteadOfRejecting() {
		// booked 10:00-11:00 — the old consumer threw and sent this event to
		// the DLT; it must now apply everything that does not conflict
		stubBookedSlotsInRange(List.of(bookedSlot(DAY.atTime(10, 0), DAY.atTime(11, 0))));
		when(therapistAvailabilityRepository.existsBySlotId(anyString())).thenReturn(false);

		consumer.process(generatedPayload(
				slot("SLT-CONFLICT", DAY.atTime(10, 0), DAY.atTime(11, 0)),
				slot("SLT-PARTIAL", DAY.atTime(10, 30), DAY.atTime(11, 30)),
				slot("SLT-CLEAR", DAY.atTime(12, 0), DAY.atTime(13, 0))));

		// AVAILABLE rows still refreshed (delete query cannot touch BOOKED rows)
		verify(therapistAvailabilityRepository).deleteAvailableSlotsInRange(
				THERAPIST_ID, DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<TherapistAvailability>> captor = ArgumentCaptor.forClass(List.class);
		verify(therapistAvailabilityRepository).saveAll(captor.capture());

		// both slots overlapping the booked time are skipped — an occupied
		// time must never be offered twice
		assertThat(captor.getValue()).hasSize(1);
		assertThat(captor.getValue().get(0).getSlotId()).isEqualTo("SLT-CLEAR");
	}

	@Test
	void generationDedupsSlotIdsThatAlreadyExist() {
		stubBookedSlotsInRange(List.of());
		when(therapistAvailabilityRepository.existsBySlotId("SLT1")).thenReturn(true);

		consumer.process(generatedPayload(slot("SLT1", DAY.atTime(9, 0), DAY.atTime(10, 0))));

		verify(therapistAvailabilityRepository, never()).saveAll(anyList());
	}

	// ── batch deletion ───────────────────────────────────────────────────────

	@Test
	void deletionWithBookedSlotsDeletesAvailableOnlyWithoutThrowing() {
		when(therapistAvailabilityRepository.existsByTherapistIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
				anyString(), eq(AvailabilityStatus.BOOKED), any(), any())).thenReturn(true);

		AvailabilitySlotsDeletedEvent event = new AvailabilitySlotsDeletedEvent();
		event.setTherapistId(THERAPIST_ID);
		event.setRangeStart(DAY);
		event.setRangeEnd(DAY);

		consumer.process(objectMapper.valueToTree(event));

		verify(therapistAvailabilityRepository).deleteAvailableSlotsInRange(
				THERAPIST_ID, DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay());
	}

	// ── single-slot events (overrides) ───────────────────────────────────────

	private JsonNode singleSlotPayload(String eventType, String slotId) {
		AvailabilityEvent event = new AvailabilityEvent();
		event.setEventType(eventType);
		event.setSlotId(slotId);
		event.setTherapistId(THERAPIST_ID);
		event.setServiceId("SRV1");
		event.setSessionFee(new BigDecimal("1500"));
		event.setStartTime(DAY.atTime(14, 0));
		event.setEndTime(DAY.atTime(15, 0));
		return objectMapper.valueToTree(event);
	}

	@Test
	void slotCreatedIsSavedWhenTimeIsClear() {
		when(therapistAvailabilityRepository.existsBySlotId("SLT-NEW")).thenReturn(false);
		when(therapistAvailabilityRepository.existsByTherapistIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
				anyString(), eq(AvailabilityStatus.BOOKED), any(), any())).thenReturn(false);

		consumer.process(singleSlotPayload("AvailabilitySlotCreated", "SLT-NEW"));

		ArgumentCaptor<TherapistAvailability> captor = ArgumentCaptor.forClass(TherapistAvailability.class);
		verify(therapistAvailabilityRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
		assertThat(captor.getValue().getSlotId()).isEqualTo("SLT-NEW");
	}

	@Test
	void slotCreatedIsSkippedWhenItOverlapsABookedSlot() {
		when(therapistAvailabilityRepository.existsBySlotId("SLT-NEW")).thenReturn(false);
		when(therapistAvailabilityRepository.existsByTherapistIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
				anyString(), eq(AvailabilityStatus.BOOKED), any(), any())).thenReturn(true);

		consumer.process(singleSlotPayload("AvailabilitySlotCreated", "SLT-NEW"));

		verify(therapistAvailabilityRepository, never()).save(any(TherapistAvailability.class));
	}

	@Test
	void slotRemovedNeverDeletesABookedSlot() {
		TherapistAvailability booked = bookedSlot(DAY.atTime(14, 0), DAY.atTime(15, 0));
		when(therapistAvailabilityRepository.findBySlotIdAndTherapistId("SLT-BOOKED", THERAPIST_ID))
				.thenReturn(Optional.of(booked));

		consumer.process(singleSlotPayload("AvailabilitySlotRemoved", "SLT-BOOKED"));

		verify(therapistAvailabilityRepository, never()).delete(any(TherapistAvailability.class));
	}

	@Test
	void slotRemovedDeletesAnAvailableSlot() {
		TherapistAvailability available = bookedSlot(DAY.atTime(14, 0), DAY.atTime(15, 0));
		available.setSlotId("SLT-FREE");
		available.setStatus(AvailabilityStatus.AVAILABLE);
		when(therapistAvailabilityRepository.findBySlotIdAndTherapistId("SLT-FREE", THERAPIST_ID))
				.thenReturn(Optional.of(available));

		consumer.process(singleSlotPayload("AvailabilitySlotRemoved", "SLT-FREE"));

		verify(therapistAvailabilityRepository).delete(available);
	}
}
