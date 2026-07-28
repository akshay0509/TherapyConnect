package com.org.appointmentService.Services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.org.appointmentService.Entity.TherapistAvailability;
import com.org.appointmentService.Enums.AvailabilityStatus;
import com.org.appointmentService.Exception.SlotAlreadyBookedException;
import com.org.appointmentService.Exception.SlotNotAvailableException;
import com.org.appointmentService.Repository.TherapistAvailabilityRepository;

import jakarta.transaction.Transactional;

/**
 * Owns the concurrency boundary for generic availability blocks.
 *
 * Rows are locked in a stable chronological order, then validated and changed
 * together. A transaction either reserves/releases the complete interval or
 * changes nothing.
 */
@Service
public class GenericBlockReservationService {

	static final int BLOCK_MINUTES = 30;

	private final TherapistAvailabilityRepository repository;

	public GenericBlockReservationService(TherapistAvailabilityRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void reserve(
			String therapistId,
			LocalDateTime start,
			LocalDateTime end,
			String appointmentId) {

		end = blockAlignedEnd(start, end);
		List<TherapistAvailability> blocks =
				repository.lockOverlappingBlocks(therapistId, start, end);
		validateExactSequence(blocks, start, end);

		for (TherapistAvailability block : blocks) {
			if (block.getStatus() != AvailabilityStatus.AVAILABLE
					|| block.getAppointmentId() != null) {
				throw new SlotAlreadyBookedException(
						"One or more required availability blocks are no longer available.");
			}
			book(block, appointmentId);
		}
		repository.saveAll(blocks);
	}

	@Transactional
	public void release(
			String therapistId,
			LocalDateTime start,
			LocalDateTime end,
			String appointmentId) {

		end = blockAlignedEnd(start, end);
		List<TherapistAvailability> blocks =
				repository.lockOverlappingBlocks(therapistId, start, end);
		validateExactSequence(blocks, start, end);
		requireOwnedBy(blocks, appointmentId);

		blocks.forEach(this::makeAvailable);
		repository.saveAll(blocks);
	}

	/**
	 * Handles partially overlapping reschedules without releasing the old
	 * interval first. The union is locked once, in one order, so blocks already
	 * owned by this appointment may be retained while new-only blocks are added.
	 */
	@Transactional
	public void move(
			String therapistId,
			LocalDateTime oldStart,
			LocalDateTime oldEnd,
			LocalDateTime newStart,
			LocalDateTime newEnd,
			String appointmentId) {

		oldEnd = blockAlignedEnd(oldStart, oldEnd);
		newEnd = blockAlignedEnd(newStart, newEnd);

		LocalDateTime unionStart = oldStart.isBefore(newStart) ? oldStart : newStart;
		LocalDateTime unionEnd = oldEnd.isAfter(newEnd) ? oldEnd : newEnd;
		List<TherapistAvailability> union =
				repository.lockOverlappingBlocks(therapistId, unionStart, unionEnd);

		List<TherapistAvailability> oldBlocks = within(union, oldStart, oldEnd);
		List<TherapistAvailability> newBlocks = within(union, newStart, newEnd);
		validateExactSequence(oldBlocks, oldStart, oldEnd);
		validateExactSequence(newBlocks, newStart, newEnd);
		requireOwnedBy(oldBlocks, appointmentId);

		for (TherapistAvailability block : newBlocks) {
			boolean available = block.getStatus() == AvailabilityStatus.AVAILABLE
					&& block.getAppointmentId() == null;
			boolean alreadyOwned = block.getStatus() == AvailabilityStatus.BOOKED
					&& appointmentId.equals(block.getAppointmentId());
			if (!available && !alreadyOwned) {
				throw new SlotAlreadyBookedException(
						"One or more required availability blocks are no longer available.");
			}
		}

		Set<String> newBlockIds = new HashSet<>();
		for (TherapistAvailability block : newBlocks) {
			newBlockIds.add(block.getSlotId());
			book(block, appointmentId);
		}
		for (TherapistAvailability block : oldBlocks) {
			if (!newBlockIds.contains(block.getSlotId())) {
				makeAvailable(block);
			}
		}
		repository.saveAll(union);
	}

	/**
	 * A session need not fill whole blocks — a 50-minute appointment occupies a
	 * 60-minute footprint on a 30-minute grid, leaving the changeover gap and
	 * keeping the next start on the clock. Callers pass the real appointment
	 * times; rounding happens here so reserve, release and move can never
	 * disagree about which blocks an appointment owns.
	 */
	static LocalDateTime blockAlignedEnd(LocalDateTime start, LocalDateTime end) {
		long minutes = Duration.between(start, end).toMinutes();
		if (minutes <= 0) {
			return end;
		}
		long blocks = (minutes + BLOCK_MINUTES - 1) / BLOCK_MINUTES;
		return start.plusMinutes(blocks * BLOCK_MINUTES);
	}

	private List<TherapistAvailability> within(
			List<TherapistAvailability> blocks,
			LocalDateTime start,
			LocalDateTime end) {
		return blocks.stream()
				.filter(block -> !block.getStartTime().isBefore(start)
						&& !block.getEndTime().isAfter(end))
				.toList();
	}

	private void validateExactSequence(
			List<TherapistAvailability> blocks,
			LocalDateTime start,
			LocalDateTime end) {

		long minutes = Duration.between(start, end).toMinutes();
		if (minutes <= 0 || minutes % BLOCK_MINUTES != 0) {
			throw new SlotNotAvailableException(
					"Requested interval must be a positive multiple of 30 minutes.");
		}

		int expectedCount = Math.toIntExact(minutes / BLOCK_MINUTES);
		if (blocks.size() != expectedCount) {
			throw new SlotNotAvailableException(
					"Requested interval does not contain enough consecutive availability blocks.");
		}

		LocalDateTime expectedStart = start;
		for (TherapistAvailability block : blocks) {
			LocalDateTime expectedEnd = expectedStart.plusMinutes(BLOCK_MINUTES);
			if (!block.getStartTime().equals(expectedStart)
					|| !block.getEndTime().equals(expectedEnd)) {
				throw new SlotNotAvailableException(
						"Requested interval contains a gap or malformed availability block.");
			}
			expectedStart = expectedEnd;
		}
	}

	private void requireOwnedBy(
			List<TherapistAvailability> blocks,
			String appointmentId) {
		for (TherapistAvailability block : blocks) {
			if (block.getStatus() != AvailabilityStatus.BOOKED
					|| !appointmentId.equals(block.getAppointmentId())) {
				throw new IllegalStateException(
						"Availability reservation ownership is inconsistent for appointment "
								+ appointmentId);
			}
		}
	}

	private void book(TherapistAvailability block, String appointmentId) {
		block.setStatus(AvailabilityStatus.BOOKED);
		block.setAppointmentId(appointmentId);
	}

	private void makeAvailable(TherapistAvailability block) {
		block.setStatus(AvailabilityStatus.AVAILABLE);
		block.setAppointmentId(null);
	}
}
