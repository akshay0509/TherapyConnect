package com.org.appointmentService.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.org.appointmentService.Entity.TherapistAvailability;
import com.org.appointmentService.Enums.AvailabilityStatus;
import com.org.appointmentService.Exception.SlotAlreadyBookedException;
import com.org.appointmentService.Exception.SlotNotAvailableException;
import com.org.appointmentService.Repository.TherapistAvailabilityRepository;

@ExtendWith(MockitoExtension.class)
class GenericBlockReservationServiceTest {

	private static final String THERAPIST_ID = "THR1";
	private static final String APPOINTMENT_ID = "APP1";
	private static final LocalDateTime NINE = LocalDateTime.of(2026, 7, 20, 9, 0);

	@Mock
	private TherapistAvailabilityRepository repository;

	@InjectMocks
	private GenericBlockReservationService service;

	@Test
	void reservesEveryConsecutiveBlockForTheSameAppointment() {
		List<TherapistAvailability> blocks = List.of(
				block("SLT1", NINE, AvailabilityStatus.AVAILABLE, null),
				block("SLT2", NINE.plusMinutes(30), AvailabilityStatus.AVAILABLE, null));
		when(repository.lockOverlappingBlocks(
				THERAPIST_ID, NINE, NINE.plusMinutes(60))).thenReturn(blocks);

		service.reserve(THERAPIST_ID, NINE, NINE.plusMinutes(60), APPOINTMENT_ID);

		assertThat(blocks).allSatisfy(block -> {
			assertThat(block.getStatus()).isEqualTo(AvailabilityStatus.BOOKED);
			assertThat(block.getAppointmentId()).isEqualTo(APPOINTMENT_ID);
		});
		verify(repository).saveAll(blocks);
	}

	@Test
	void sessionShorterThanItsBlocksStillReservesTheWholeFootprint() {
		// A 50-minute session on a 30-minute grid holds 09:00-10:00, so the next
		// session still starts on the clock rather than at 09:50.
		List<TherapistAvailability> blocks = List.of(
				block("SLT1", NINE, AvailabilityStatus.AVAILABLE, null),
				block("SLT2", NINE.plusMinutes(30), AvailabilityStatus.AVAILABLE, null));
		when(repository.lockOverlappingBlocks(
				THERAPIST_ID, NINE, NINE.plusMinutes(60))).thenReturn(blocks);

		service.reserve(THERAPIST_ID, NINE, NINE.plusMinutes(50), APPOINTMENT_ID);

		assertThat(blocks).allSatisfy(block -> {
			assertThat(block.getStatus()).isEqualTo(AvailabilityStatus.BOOKED);
			assertThat(block.getAppointmentId()).isEqualTo(APPOINTMENT_ID);
		});
	}

	@Test
	void releaseFreesTheSameFootprintReserveTook() {
		// Cancelling passes the appointment's own 50-minute span; it must resolve
		// to the identical two blocks or the release would not find its owner.
		List<TherapistAvailability> blocks = List.of(
				block("SLT1", NINE, AvailabilityStatus.BOOKED, APPOINTMENT_ID),
				block("SLT2", NINE.plusMinutes(30), AvailabilityStatus.BOOKED, APPOINTMENT_ID));
		when(repository.lockOverlappingBlocks(
				THERAPIST_ID, NINE, NINE.plusMinutes(60))).thenReturn(blocks);

		service.release(THERAPIST_ID, NINE, NINE.plusMinutes(50), APPOINTMENT_ID);

		assertThat(blocks).allSatisfy(block -> {
			assertThat(block.getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
			assertThat(block.getAppointmentId()).isNull();
		});
	}

	@Test
	void reservationIsRejectedWhenARequiredBlockIsMissing() {
		List<TherapistAvailability> incomplete = List.of(
				block("SLT1", NINE, AvailabilityStatus.AVAILABLE, null));
		when(repository.lockOverlappingBlocks(
				THERAPIST_ID, NINE, NINE.plusMinutes(60))).thenReturn(incomplete);

		assertThrows(SlotNotAvailableException.class,
				() -> service.reserve(
						THERAPIST_ID, NINE, NINE.plusMinutes(60), APPOINTMENT_ID));
		verify(repository, never()).saveAll(anyList());
	}

	@Test
	void reservationIsRejectedWhenAnotherAppointmentOwnsAnyBlock() {
		List<TherapistAvailability> blocks = List.of(
				block("SLT1", NINE, AvailabilityStatus.AVAILABLE, null),
				block("SLT2", NINE.plusMinutes(30), AvailabilityStatus.BOOKED, "APP2"));
		when(repository.lockOverlappingBlocks(
				THERAPIST_ID, NINE, NINE.plusMinutes(60))).thenReturn(blocks);

		assertThrows(SlotAlreadyBookedException.class,
				() -> service.reserve(
						THERAPIST_ID, NINE, NINE.plusMinutes(60), APPOINTMENT_ID));
		verify(repository, never()).saveAll(anyList());
	}

	@Test
	void partiallyOverlappingMoveRetainsSharedBlockAndReleasesOldOnlyBlock() {
		TherapistAvailability oldOnly =
				block("SLT1", NINE, AvailabilityStatus.BOOKED, APPOINTMENT_ID);
		TherapistAvailability shared =
				block("SLT2", NINE.plusMinutes(30), AvailabilityStatus.BOOKED, APPOINTMENT_ID);
		TherapistAvailability newOnly =
				block("SLT3", NINE.plusMinutes(60), AvailabilityStatus.AVAILABLE, null);
		List<TherapistAvailability> union = List.of(oldOnly, shared, newOnly);
		when(repository.lockOverlappingBlocks(
				THERAPIST_ID, NINE, NINE.plusMinutes(90))).thenReturn(union);

		service.move(
				THERAPIST_ID,
				NINE,
				NINE.plusMinutes(60),
				NINE.plusMinutes(30),
				NINE.plusMinutes(90),
				APPOINTMENT_ID);

		assertThat(oldOnly.getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
		assertThat(oldOnly.getAppointmentId()).isNull();
		assertThat(shared.getStatus()).isEqualTo(AvailabilityStatus.BOOKED);
		assertThat(shared.getAppointmentId()).isEqualTo(APPOINTMENT_ID);
		assertThat(newOnly.getStatus()).isEqualTo(AvailabilityStatus.BOOKED);
		assertThat(newOnly.getAppointmentId()).isEqualTo(APPOINTMENT_ID);
		verify(repository).saveAll(union);
	}

	private TherapistAvailability block(
			String slotId,
			LocalDateTime start,
			AvailabilityStatus status,
			String appointmentId) {
		TherapistAvailability block = new TherapistAvailability();
		block.setSlotId(slotId);
		block.setTherapistId(THERAPIST_ID);
		block.setStartTime(start);
		block.setEndTime(start.plusMinutes(30));
		block.setStatus(status);
		block.setAppointmentId(appointmentId);
		return block;
	}
}
