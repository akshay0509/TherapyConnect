package com.org.therapistService.Scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.org.therapistService.Entity.ClientDto;
import com.org.therapistService.Entity.ClientReminderDto;
import com.org.therapistService.Entity.TherapistAppointments;
import com.org.therapistService.Enums.AppointmentStatus;
import com.org.therapistService.Messaging.EmailReminderProducer;
import com.org.therapistService.Proxy.ClientServiceProxy;
import com.org.therapistService.Repository.TherapistAppointmentsRepository;

@Service
public class EmailReminderScheduler {
	
	@Autowired
	EmailReminderProducer emailReminderProducer;
	
	@Autowired
	TherapistAppointmentsRepository therapistAppointmentsRepository;
	
	@Autowired
	ClientServiceProxy clientServiceProxy;

	@Scheduled(cron = "0 0 19 * * *")
	public void publishEmailReminders() {
		ClientReminderDto clientReminderDto;
		LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
		List<TherapistAppointments> therapistAppointmentsList = therapistAppointmentsRepository.findByReminderSentFalseAndStatusAndStartTimeBetween(AppointmentStatus.SCHEDULED, LocalDateTime.now(), tomorrow);
		
		for(TherapistAppointments therapistAppointment : therapistAppointmentsList) {
			ClientDto clientDto = clientServiceProxy.getClient(therapistAppointment.getClientId());
			clientReminderDto = new ClientReminderDto(clientDto.getEmail(), therapistAppointment.getStartTime());
			
			emailReminderProducer.sendMessage(therapistAppointment.getClientId(), clientReminderDto);
		}
		
	}
}
