package com.org.appointmentService.Scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.org.appointmentService.Enums.AvailabilityStatus;
import com.org.appointmentService.Repository.TherapistAvailabilityRepository;

/**
 * The purge must only ever remove expired AVAILABLE slots — expired BOOKED
 * slots anchor the calendar-history join (findEffectiveSlotsWithAppointment)
 * and deleting them would erase past appointments from the calendar.
 */
@ExtendWith(MockitoExtension.class)
class PastSlotPurgeSchedulerTest {

	@Mock
	private TherapistAvailabilityRepository therapistAvailabilityRepository;

	@InjectMocks
	private PastSlotPurgeScheduler scheduler;

	@Test
	void purgeTargetsOnlyAvailableSlotsWithThirtyDayRetention() {
		when(therapistAvailabilityRepository.deleteByStatusAndEndTimeBefore(
				eq(AvailabilityStatus.AVAILABLE), any())).thenReturn(12L);

		scheduler.purgePastAvailableSlots();

		ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(therapistAvailabilityRepository).deleteByStatusAndEndTimeBefore(
				eq(AvailabilityStatus.AVAILABLE), cutoff.capture());

		LocalDateTime now = LocalDateTime.now();
		assertThat(cutoff.getValue()).isAfter(now.minusDays(31));
		assertThat(cutoff.getValue()).isBefore(now.minusDays(29));
	}
}
