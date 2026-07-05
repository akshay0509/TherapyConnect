package com.org.therapistService.Scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.org.therapistService.Repository.TherapistAvailabilityRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;
import com.org.therapistService.Services.AvailabilitySlotService;

import jakarta.transaction.Transactional;

@Service
public class TherapistAvailabilityGeneratorScheduler {

	@Autowired
    private TherapistAvailabilityRulesRepository therapistAvailabilityRulesRepository;

    @Autowired
    private AvailabilitySlotService availabilitySlotService;

    @Autowired
    private TherapistAvailabilityRepository therapistAvailabilityRepository;

	private static final int PAST_SLOT_RETENTION_DAYS = 30;

	private static final Logger logger = LoggerFactory.getLogger(TherapistAvailabilityGeneratorScheduler.class);

	// Horizon extension only: fills days that have no slots yet (normally just
	// the day newly entering the 7-day window). Rule changes regenerate
	// immediately at edit time, so already-generated days are never re-chopped
	// here — that would only churn slotIds and projection events nightly.
	@Scheduled(cron = "0 0 2 * * *")
	public void generateTherapistAvailabilitySlots() {

		List<String> therapistIds = therapistAvailabilityRulesRepository.findAllDistinctTherapistIds();
		LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(6);

		for (String therapistId : therapistIds) {
			try {
				availabilitySlotService.generateMissingDays(therapistId, startDate, endDate);
			}
			catch (Exception e) {
				// one therapist failing must never abort the run for the rest
				logger.error("Nightly slot generation failed for therapistId={}", therapistId, e);
			}
		}
	}

	// Generation only ever works from today forward, so past slot rows are
	// never read again; without a purge the table grows unbounded. This table
	// has no status column — appointment history lives in the appointment
	// tables, not here — so all expired rows are safe to drop.
	@Scheduled(cron = "0 0 4 * * *")
	@Transactional
	public void purgePastSlots() {
		long deleted = therapistAvailabilityRepository.deleteByEndTimeBefore(
				LocalDateTime.now().minusDays(PAST_SLOT_RETENTION_DAYS));
		if (deleted > 0) {
			logger.info("Purged {} availability slots older than {} days", deleted, PAST_SLOT_RETENTION_DAYS);
		}
	}
}
