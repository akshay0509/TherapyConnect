package com.org.appointmentService.Scheduler;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.org.appointmentService.Enums.AvailabilityStatus;
import com.org.appointmentService.Repository.TherapistAvailabilityRepository;

import jakarta.transaction.Transactional;

@Service
public class PastSlotPurgeScheduler {

	@Autowired
	private TherapistAvailabilityRepository therapistAvailabilityRepository;

	private static final int PAST_SLOT_RETENTION_DAYS = 30;

	private static final Logger logger = LoggerFactory.getLogger(PastSlotPurgeScheduler.class);

	// Expired AVAILABLE slots carry no information; without a purge the
	// projection grows unbounded. BOOKED slots are deliberately kept — the
	// calendar history view joins appointments through them
	// (findEffectiveSlotsWithAppointment), so deleting them would erase past
	// appointments from the calendar.
	@Scheduled(cron = "0 0 4 * * *")
	@Transactional
	public void purgePastAvailableSlots() {
		long deleted = therapistAvailabilityRepository.deleteByStatusAndEndTimeBefore(
				AvailabilityStatus.AVAILABLE,
				LocalDateTime.now().minusDays(PAST_SLOT_RETENTION_DAYS));
		if (deleted > 0) {
			logger.info("Purged {} past AVAILABLE slots older than {} days", deleted, PAST_SLOT_RETENTION_DAYS);
		}
	}
}
