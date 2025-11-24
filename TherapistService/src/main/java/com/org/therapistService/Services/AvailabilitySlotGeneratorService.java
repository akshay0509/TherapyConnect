package com.org.therapistService.Services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.org.therapistService.Entity.TherapistAvailabilityOverrides;
import com.org.therapistService.Entity.TherapistAvailabilityRules;
import com.org.therapistService.Repository.TherapistAvailabilityOverridesRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;

@Service
public class AvailabilitySlotGeneratorService {

	@Autowired
	private TherapistAvailabilityRulesRepository therapistAvailabilityRulesRepository;

	@Autowired
	private TherapistAvailabilityOverridesRepository therapistAvailabilityOverridesRepository;

	public void generateTherapistAvailabilitySlots(String therapistId, LocalDate startDate, LocalDate endDate) {

		List<TherapistAvailabilityOverrides> overrides = therapistAvailabilityOverridesRepository.findByTherapistIdAndStartTimeBetween(therapistId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
		for(LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			int dayOfWeek = date.getDayOfWeek().getValue();
			List<TherapistAvailabilityRules> rulesForDay = therapistAvailabilityRulesRepository.findByTherapistIdAndDayOfWeekAndIsActiveTrue(therapistId, dayOfWeek);
		}
	}
}
