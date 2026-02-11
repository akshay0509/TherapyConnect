package com.org.therapistService.Messaging;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.org.events.TherapistAvailability.AvailabilitySlotsGeneratedEvent;
import com.org.events.TherapistAvailability.Slot;
import com.org.therapistService.Entity.TherapistAvailability;

@Service
public class TherapistAvailabilityProducer {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private static final String topic = "therapist-availability-events";
	
	public void sendMessage(String therapistId, LocalDate startDate, LocalDate endDate, List<TherapistAvailability> therapistAvailabilityList) {
		
		AvailabilitySlotsGeneratedEvent availabilitySlotsGeneratedEvent = new AvailabilitySlotsGeneratedEvent();
		availabilitySlotsGeneratedEvent.setTherapistId(therapistId);
		availabilitySlotsGeneratedEvent.setRangeStart(startDate);
		availabilitySlotsGeneratedEvent.setRangeEnd(endDate);
		
		List<Slot> slotList = new ArrayList<>();
		for(TherapistAvailability therapistAvailability : therapistAvailabilityList) {
			Slot slot = new Slot();
			slot.setSlotId(therapistAvailability.getSlotId());
			slot.setStartTime(therapistAvailability.getStartTime());
			slot.setEndTime(therapistAvailability.getEndTime());
			
			slotList.add(slot);
		}
		
		availabilitySlotsGeneratedEvent.setSlotList(slotList);
		
		kafkaTemplate.send(topic, therapistId, availabilitySlotsGeneratedEvent);
	}
}
