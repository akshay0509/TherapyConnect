package com.org.therapistService.Scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.org.therapistService.Repository.TherapistAvailabilityRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;
import com.org.therapistService.Services.AvailabilitySlotService;

/**
 * Verifies the nightly scheduler contract:
 * - uses horizon-extension (generateMissingDays), never full regeneration
 * - one therapist failing does not abort the run for the rest (C3 fix)
 * - past-slot purge deletes rows ended before the 30-day retention cutoff
 */
@ExtendWith(MockitoExtension.class)
class TherapistAvailabilityGeneratorSchedulerTest {

	@Mock
	private TherapistAvailabilityRulesRepository therapistAvailabilityRulesRepository;

	@Mock
	private AvailabilitySlotService availabilitySlotService;

	@Mock
	private TherapistAvailabilityRepository therapistAvailabilityRepository;

	@InjectMocks
	private TherapistAvailabilityGeneratorScheduler scheduler;

	@Test
	void nightlyRunUsesMissingDaysGenerationForTheSevenDayWindow() throws Exception {
		when(therapistAvailabilityRulesRepository.findAllDistinctTherapistIds())
				.thenReturn(List.of("THR1"));

		scheduler.generateTherapistAvailabilitySlots();

		LocalDate today = LocalDate.now();
		verify(availabilitySlotService).generateMissingDays("THR1", today, today.plusDays(6));
		// the nightly job must never full-regenerate — that churns slotIds
		verify(availabilitySlotService, never()).generateAvailabilitySlots(anyString(), any(), any());
	}

	@Test
	void oneTherapistFailingDoesNotAbortTheRunForTheRest() throws Exception {
		when(therapistAvailabilityRulesRepository.findAllDistinctTherapistIds())
				.thenReturn(List.of("THR1", "THR2", "THR3"));
		doThrow(new RuntimeException("db hiccup"))
				.when(availabilitySlotService).generateMissingDays(eq("THR1"), any(), any());

		scheduler.generateTherapistAvailabilitySlots();

		verify(availabilitySlotService).generateMissingDays(eq("THR2"), any(), any());
		verify(availabilitySlotService).generateMissingDays(eq("THR3"), any(), any());
	}

	@Test
	void purgeDeletesSlotsEndedBeforeThirtyDayRetention() {
		when(therapistAvailabilityRepository.deleteByEndTimeBefore(any())).thenReturn(42L);

		scheduler.purgePastSlots();

		ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(therapistAvailabilityRepository).deleteByEndTimeBefore(cutoff.capture());

		LocalDateTime now = LocalDateTime.now();
		assertThat(cutoff.getValue()).isAfter(now.minusDays(31));
		assertThat(cutoff.getValue()).isBefore(now.minusDays(29));
	}
}
