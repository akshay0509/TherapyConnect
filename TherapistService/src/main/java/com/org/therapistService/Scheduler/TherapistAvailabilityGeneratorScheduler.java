package com.org.therapistService.Scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;
import com.org.therapistService.Services.AvailabilitySlotService;

@Service
public class TherapistAvailabilityGeneratorScheduler {
	
	@Autowired
    private TherapistAvailabilityRulesRepository therapistAvailabilityRulesRepository;

    @Autowired
    private AvailabilitySlotService availabilitySlotService;

	@Scheduled(cron = "0 0 2 * * *")
	public void generateTherapistAvailabilitySlots() {
		
		List<String> therapistIds = therapistAvailabilityRulesRepository.findAllDistinctTherapistIds();
		LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(6);
		
		for(String therapistId : therapistIds) {
			try {
				availabilitySlotService.generateAvailabilitySlots(therapistId, startDate, endDate);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
