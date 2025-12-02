package com.org.therapistService.Scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;
import com.org.therapistService.Services.AvailabilitySlotGeneratorService;

@Service
public class TherapistAvailabilityGeneratorScheduler {
	
	@Autowired
    private TherapistAvailabilityRulesRepository therapistAvailabilityRulesRepository;

    @Autowired
    private AvailabilitySlotGeneratorService availabilitySlotGeneratorService;

	@Scheduled(cron = "0 0 2 * * *")
	public void generateTherapistAvailabilitySlots() {
		
		List<String> therapistIds = therapistAvailabilityRulesRepository.findAllDistinctTherapistIds();
		LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(6);
		
		for(String therapistId : therapistIds) {
			availabilitySlotGeneratorService.generateTherapistAvailabilitySlots(therapistId, startDate, endDate);
		}
	}
}
