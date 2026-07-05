package com.org.therapistService.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.org.therapistService.Entity.TherapistAvailability;
import com.org.therapistService.Entity.TherapistAvailabilityOverrides;
import com.org.therapistService.Entity.TherapistAvailabilityRules;
import com.org.therapistService.Entity.TherapistServices;
import com.org.therapistService.Repository.AppointmentProjectionRepository;
import com.org.therapistService.Repository.TherapistAvailabilityOverridesRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;
import com.org.therapistService.Repository.TherapistServicesRepository;

/**
 * Unit tests for the per-day generation redesign:
 * - generateAvailabilitySlots (manual / rule-change path): regenerates free
 *   days, skips days with active appointments, publishes one event per
 *   regenerated day (including empty days — destructive sync).
 * - generateMissingDays (nightly path): fills only slot-less days, never
 *   re-chops existing days, publishes nothing for days that yield nothing.
 * - slot chopping math and appointment-overlap guards.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvailabilitySlotServiceTest {

	@Mock
	private TherapistAvailabilityRulesRepository therapistAvailabilityRulesRepository;

	@Mock
	private TherapistAvailabilityOverridesRepository therapistAvailabilityOverridesRepository;

	@Mock
	private TherapistAvailabilityRepository therapistAvailabilityRepository;

	@Mock
	private TherapistServicesRepository therapistServicesRepository;

	@Mock
	private OutboxService outboxService;

	@Mock
	private AppointmentProjectionRepository appointmentProjectionRepository;

	@InjectMocks
	private AvailabilitySlotService availabilitySlotService;

	private static final String THERAPIST_ID = "THR12345";
	// fixed Monday so dayOfWeek-based rules are deterministic
	private static final LocalDate MONDAY = LocalDate.of(2026, 7, 13);

	private TherapistServices service60min() {
		TherapistServices service = new TherapistServices();
		service.setServiceId("SRV1");
		service.setTherapistId(THERAPIST_ID);
		service.setDuration(60);
		service.setPrice(new BigDecimal("1500"));
		service.setActive(true);
		return service;
	}

	private TherapistAvailabilityRules mondayRule(LocalTime start, LocalTime end) {
		TherapistAvailabilityRules rule = new TherapistAvailabilityRules();
		rule.setTherapistId(THERAPIST_ID);
		rule.setDayOfWeek(1); // Monday
		rule.setStartTime(start);
		rule.setEndTime(end);
		rule.setActive(true);
		return rule;
	}

	private TherapistAvailability slotAt(LocalDateTime start, LocalDateTime end) {
		TherapistAvailability slot = new TherapistAvailability();
		slot.setSlotId("SLT-EXISTING");
		slot.setTherapistId(THERAPIST_ID);
		slot.setStartTime(start);
		slot.setEndTime(end);
		slot.setServiceId("SRV1");
		slot.setSessionFee(new BigDecimal("1500"));
		return slot;
	}

	private void stubNoActiveAppointments() {
		when(appointmentProjectionRepository.existsByTherapistIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
				anyString(), anyList(), any(), any())).thenReturn(false);
	}

	private void stubActiveAppointmentsEverywhere() {
		when(appointmentProjectionRepository.existsByTherapistIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
				anyString(), anyList(), any(), any())).thenReturn(true);
	}

	private void stubEmptyOverrides() {
		when(therapistAvailabilityOverridesRepository.findByTherapistIdAndStartTimeBetweenOrderByStartTimeAsc(
				anyString(), any(), any())).thenReturn(List.of());
	}

	// ── generateAvailabilitySlots (manual / rule-change path) ───────────────

	@Test
	void manualGenerationRegeneratesFreeDayAndChopsSlotsCorrectly() throws Exception {
		stubNoActiveAppointments();
		stubEmptyOverrides();
		when(therapistServicesRepository.findByTherapistIdAndIsActiveTrue(THERAPIST_ID))
				.thenReturn(List.of(service60min()));
		when(therapistAvailabilityRulesRepository.findByTherapistIdAndIsActiveTrue(THERAPIST_ID))
				.thenReturn(List.of(mondayRule(LocalTime.of(9, 0), LocalTime.of(11, 0))));
		when(therapistAvailabilityRepository.saveAll(anyList()))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(therapistAvailabilityRepository.findByTherapistIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
				anyString(), any(), any())).thenReturn(List.of());

		availabilitySlotService.generateAvailabilitySlots(THERAPIST_ID, MONDAY, MONDAY);

		// day is deleted and rebuilt
		verify(therapistAvailabilityRepository).deleteInRange(
				THERAPIST_ID, MONDAY.atStartOfDay(), MONDAY.plusDays(1).atStartOfDay());

		// 9:00-11:00 with 60-min service and 30-min stride → 9:00, 9:30, 10:00
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<TherapistAvailability>> captor = ArgumentCaptor.forClass(List.class);
		verify(therapistAvailabilityRepository).saveAll(captor.capture());
		List<TherapistAvailability> saved = captor.getValue();

		assertThat(saved).hasSize(3);
		assertThat(saved).extracting(TherapistAvailability::getStartTime).containsExactly(
				MONDAY.atTime(9, 0), MONDAY.atTime(9, 30), MONDAY.atTime(10, 0));
		assertThat(saved).extracting(TherapistAvailability::getEndTime).containsExactly(
				MONDAY.atTime(10, 0), MONDAY.atTime(10, 30), MONDAY.atTime(11, 0));
		assertThat(saved).allSatisfy(slot -> {
			assertThat(slot.getSessionFee()).isEqualByComparingTo("1500");
			assertThat(slot.getServiceId()).isEqualTo("SRV1");
		});

		verify(outboxService).saveOutboxEvent(
				eq("THERAPIST_AVAILABILITY"), eq(THERAPIST_ID), eq("AvailabilitySlotsGenerated"), any());
	}

	@Test
	void manualGenerationSkipsDayWithActiveAppointmentAndKeepsItsSlots() throws Exception {
		stubActiveAppointmentsEverywhere();
		TherapistAvailability existing = slotAt(MONDAY.atTime(9, 0), MONDAY.atTime(10, 0));
		when(therapistAvailabilityRepository.findByTherapistIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
				anyString(), any(), any())).thenReturn(List.of(existing));

		List<TherapistAvailability> result =
				availabilitySlotService.generateAvailabilitySlots(THERAPIST_ID, MONDAY, MONDAY);

		// nothing deleted, nothing published, existing slots returned untouched
		verify(therapistAvailabilityRepository, never()).deleteInRange(anyString(), any(), any());
		verifyNoInteractions(outboxService);
		assertThat(result).containsExactly(existing);
	}

	@Test
	void manualGenerationPublishesOneEventPerDayIncludingEmptyDays() throws Exception {
		// Monday has a rule; Tuesday has none → Tuesday still publishes an
		// (empty) event so the projection clears any removed slots
		stubNoActiveAppointments();
		stubEmptyOverrides();
		when(therapistServicesRepository.findByTherapistIdAndIsActiveTrue(THERAPIST_ID))
				.thenReturn(List.of(service60min()));
		when(therapistAvailabilityRulesRepository.findByTherapistIdAndIsActiveTrue(THERAPIST_ID))
				.thenReturn(List.of(mondayRule(LocalTime.of(9, 0), LocalTime.of(10, 0))));
		when(therapistAvailabilityRepository.saveAll(anyList()))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(therapistAvailabilityRepository.findByTherapistIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
				anyString(), any(), any())).thenReturn(List.of());

		availabilitySlotService.generateAvailabilitySlots(THERAPIST_ID, MONDAY, MONDAY.plusDays(1));

		verify(therapistAvailabilityRepository, times(2)).deleteInRange(anyString(), any(), any());
		verify(outboxService, times(2)).saveOutboxEvent(
				eq("THERAPIST_AVAILABILITY"), eq(THERAPIST_ID), eq("AvailabilitySlotsGenerated"), any());
	}

	// ── generateMissingDays (nightly path) ──────────────────────────────────

	@Test
	void nightlyGenerationSkipsDayThatAlreadyHasSlots() throws Exception {
		when(therapistAvailabilityRepository.findByTherapistIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
				anyString(), any(), any()))
				.thenReturn(List.of(slotAt(MONDAY.atTime(9, 0), MONDAY.atTime(10, 0))));

		availabilitySlotService.generateMissingDays(THERAPIST_ID, MONDAY, MONDAY);

		verify(therapistAvailabilityRepository, never()).deleteInRange(anyString(), any(), any());
		verifyNoInteractions(outboxService);
	}

	@Test
	void nightlyGenerationSkipsDayWithActiveAppointment() throws Exception {
		when(therapistAvailabilityRepository.findByTherapistIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
				anyString(), any(), any())).thenReturn(List.of());
		stubActiveAppointmentsEverywhere();

		availabilitySlotService.generateMissingDays(THERAPIST_ID, MONDAY, MONDAY);

		verify(therapistAvailabilityRepository, never()).deleteInRange(anyString(), any(), any());
		verifyNoInteractions(outboxService);
	}

	@Test
	void nightlyGenerationFillsEmptyDayAndPublishes() throws Exception {
		stubNoActiveAppointments();
		stubEmptyOverrides();
		when(therapistServicesRepository.findByTherapistIdAndIsActiveTrue(THERAPIST_ID))
				.thenReturn(List.of(service60min()));
		when(therapistAvailabilityRulesRepository.findByTherapistIdAndIsActiveTrue(THERAPIST_ID))
				.thenReturn(List.of(mondayRule(LocalTime.of(9, 0), LocalTime.of(10, 0))));
		when(therapistAvailabilityRepository.saveAll(anyList()))
				.thenAnswer(invocation -> invocation.getArgument(0));
		// first find = emptiness check (no slots), second find = post-regen read
		when(therapistAvailabilityRepository.findByTherapistIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
				anyString(), any(), any()))
				.thenReturn(List.of(), List.of(slotAt(MONDAY.atTime(9, 0), MONDAY.atTime(10, 0))));

		availabilitySlotService.generateMissingDays(THERAPIST_ID, MONDAY, MONDAY);

		verify(therapistAvailabilityRepository).deleteInRange(
				THERAPIST_ID, MONDAY.atStartOfDay(), MONDAY.plusDays(1).atStartOfDay());
		verify(outboxService).saveOutboxEvent(
				eq("THERAPIST_AVAILABILITY"), eq(THERAPIST_ID), eq("AvailabilitySlotsGenerated"), any());
	}

	@Test
	void nightlyGenerationPublishesNothingWhenDayYieldsNoSlots() throws Exception {
		// no active services → no slots can be generated for the day
		stubNoActiveAppointments();
		stubEmptyOverrides();
		when(therapistServicesRepository.findByTherapistIdAndIsActiveTrue(THERAPIST_ID))
				.thenReturn(List.of());
		when(therapistAvailabilityRepository.findByTherapistIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
				anyString(), any(), any())).thenReturn(List.of());

		availabilitySlotService.generateMissingDays(THERAPIST_ID, MONDAY, MONDAY);

		verifyNoInteractions(outboxService);
	}

	// ── destructive-action guards ───────────────────────────────────────────

	@Test
	void deleteSlotsRangeIsRejectedWhenActiveAppointmentsOverlap() {
		stubActiveAppointmentsEverywhere();

		assertThrows(IllegalStateException.class,
				() -> availabilitySlotService.deleteAvailabilitySlots(THERAPIST_ID, MONDAY, MONDAY.plusDays(6)));

		verify(therapistAvailabilityRepository, never()).deleteInRange(anyString(), any(), any());
		verifyNoInteractions(outboxService);
	}

	@Test
	void createdOverrideIsRejectedWhenActiveAppointmentsOverlap() {
		stubActiveAppointmentsEverywhere();

		TherapistAvailabilityOverrides override = new TherapistAvailabilityOverrides();
		override.setTherapistId(THERAPIST_ID);
		override.setStartTime(MONDAY.atTime(14, 0));
		override.setEndTime(MONDAY.atTime(16, 0));
		override.setAvailable(true);

		assertThrows(IllegalStateException.class,
				() -> availabilitySlotService.applyCreatedOverride(THERAPIST_ID, override));

		verify(therapistAvailabilityRepository, never()).saveAll(anyList());
		verifyNoInteractions(outboxService);
	}
}
