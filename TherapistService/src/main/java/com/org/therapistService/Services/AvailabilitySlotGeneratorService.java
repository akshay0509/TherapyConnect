package com.org.therapistService.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.org.therapistService.Entity.TherapistAvailability;
import com.org.therapistService.Entity.TherapistAvailabilityOverrides;
import com.org.therapistService.Entity.TherapistAvailabilityRules;
import com.org.therapistService.Entity.TherapistServices;
import com.org.therapistService.Enums.SessionType;
import com.org.therapistService.Repository.TherapistAvailabilityOverridesRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRepository;
import com.org.therapistService.Repository.TherapistAvailabilityRulesRepository;
import com.org.therapistService.Repository.TherapistServicesRepository;

@Service
public class AvailabilitySlotGeneratorService {

	@Autowired
	private TherapistAvailabilityRulesRepository therapistAvailabilityRulesRepository;

	@Autowired
	private TherapistAvailabilityOverridesRepository therapistAvailabilityOverridesRepository;

	@Autowired
	private TherapistAvailabilityRepository therapistAvailabilityRepository;
	
	@Autowired
	private TherapistServicesRepository therapistServicesRepository;
	
	private static final Logger logger = LogManager.getLogger(AvailabilitySlotGeneratorService.class);

	public List<TherapistAvailability> generateTherapistAvailabilitySlots(String therapistId, LocalDate startDate, LocalDate endDate) {

		List<TherapistAvailability> newSlotsToSave = new ArrayList<>();
		
		List<TherapistServices> therapistServices = therapistServicesRepository.findByTherapistIdAndIsActiveTrue(therapistId);

		// If the therapist has no active services, we can't generate anything
		if (therapistServices.isEmpty()) {
			return new ArrayList<>(); 
		}

		List<TherapistAvailabilityOverrides> overrides = therapistAvailabilityOverridesRepository.findByTherapistIdAndStartTimeBetween(therapistId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

		for(LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

			LocalDate finalDate = date;
			boolean isUnavailable = overrides.stream().anyMatch(o ->
			!o.isAvailable() && o.getStartTime().toLocalDate().equals(finalDate));

			if(isUnavailable) {
				continue;
			}
			int dayOfWeek = date.getDayOfWeek().getValue();
			List<TherapistAvailabilityRules> rulesForDay = therapistAvailabilityRulesRepository.findByTherapistIdAndDayOfWeekAndIsActiveTrue(therapistId, dayOfWeek);

			for (TherapistAvailabilityRules rule : rulesForDay) {
				newSlotsToSave.addAll(
						chopTimeBlockIntoSlots(therapistId, finalDate, rule.getStartTime(), rule.getEndTime(), rule.getSessionType(), therapistServices)
						);
			}

			// 7. Find and process "AVAILABLE" overrides for this day
			// Usage: We use 'finalDate' here as well
			overrides.stream()
			.filter(o -> o.isAvailable() && o.getStartTime().toLocalDate().equals(finalDate))
			.forEach(override -> newSlotsToSave.addAll(
					chopTimeBlockIntoSlots(
							therapistId, 
							finalDate, 
							override.getStartTime().toLocalTime(), 
							override.getEndTime().toLocalTime(),
							override.getSessionType(),
							therapistServices
							)
					));
		}
		// 8. Save all new slots in a single batch
		if (!newSlotsToSave.isEmpty()) {
			return therapistAvailabilityRepository.saveAll(newSlotsToSave);
		}

		return new ArrayList<>();

	}

	private List<TherapistAvailability> chopTimeBlockIntoSlots(String therapistId, LocalDate date, LocalTime startTime, LocalTime endTime, SessionType sessionType, List<TherapistServices> therapistServices) {

		List<TherapistAvailability> newSlots = new ArrayList<>();

        // Iterate through ALL of the therapist's available services
        for (TherapistServices service : therapistServices) {
            
            // The duration for this specific service
            int slotDurationMinutes = service.getDuration();
            String serviceId = service.getServiceId();
            
            LocalTime currentSlotTime = startTime;

            // Start chopping the block using the service's duration
            while (currentSlotTime.isBefore(endTime)) {
                
                LocalDateTime slotStartTime = LocalDateTime.of(date, currentSlotTime);
                LocalDateTime slotEndTime = slotStartTime.plusMinutes(slotDurationMinutes);

                // Stop if the slot extends past the end of the time block
                if (slotEndTime.isAfter(LocalDateTime.of(date, endTime))) {
                    break;
                }

                // CRITICAL CHECK: Prevent duplicates by checking therapist/time
                // We don't check serviceId here because if one service books the time, 
                // NO service can book that exact start time.
                if (!therapistAvailabilityRepository.existsByTherapistIdAndStartTime(therapistId, slotStartTime)) {
                    
                	TherapistAvailability slot = new TherapistAvailability();
                    slot.setTherapistId(therapistId);
                    slot.setStartTime(slotStartTime);
                    slot.setEndTime(slotEndTime);
                    slot.setServiceId(serviceId); // <-- Now links to the specific service
                    slot.setSessionType(sessionType); 

                    newSlots.add(slot);
                }

                // Move to the next potential start time
                currentSlotTime = currentSlotTime.plusMinutes(slotDurationMinutes);
            }
        }
        return newSlots;
	}
}
