package com.org.therapistService.Scheduler;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.org.therapistService.Repository.TherapistAvailabilityOverridesRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;

@Service
public class TherapistAvailabilityGenerator {
	
	@Autowired
    private TherapistAvailabilityRulesRepository therapistAvailabilityRulesRepository;
	
    @Autowired
    private TherapistAvailabilityOverridesRepository therapistAvailabilityOverridesRepository;
    
    @Autowired
    private TherapistAvailabilityRepository therapistAvailabilityRepository;

	@Scheduled(cron = "0 0 2 * * *")
	public void generateTherapistAvailability() {
		
		List<String> therapistIds = therapistAvailabilityRulesRepository.findAllDistinctTherapistIds();
		
		for(String therapist : therapistIds) {
			
		}
	}
}
