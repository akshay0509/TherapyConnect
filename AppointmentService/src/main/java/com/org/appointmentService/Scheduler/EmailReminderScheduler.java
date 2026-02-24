package com.org.appointmentService.Scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.org.appointmentService.Entity.TherapistAppointments;
import com.org.appointmentService.Enums.AppointmentStatus;
import com.org.appointmentService.Messaging.Producer.EmailReminderProducer;
import com.org.appointmentService.Repository.TherapistAppointmentsRepository;
import com.org.events.email.EmailReminderEvent;


@Service
public class EmailReminderScheduler {
	
	@Autowired
	EmailReminderProducer emailReminderProducer;
	
	@Autowired
	TherapistAppointmentsRepository therapistAppointmentsRepository;
	
	/*
	@Autowired
	ClientServiceProxy clientServiceProxy;
	*/
	
	@Scheduled(cron = "0 0 19 * * *")
	public void publishEmailReminders() {
		EmailReminderEvent emailReminderEvent;
		LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
		/*
		List<TherapistAppointments> therapistAppointmentsList = therapistAppointmentsRepository.findByReminderSentFalseAndStatusAndStartTimeBetween(AppointmentStatus.SCHEDULED, LocalDateTime.now(), tomorrow);
		
		for(TherapistAppointments therapistAppointment : therapistAppointmentsList) {
			
			ClientDto clientDto = clientServiceProxy.getClient(therapistAppointment.getClientId());
			emailReminderEvent = new EmailReminderEvent();
			emailReminderEvent.setEmail(clientDto.getEmail());
			emailReminderEvent.setAppointmentTime(therapistAppointment.getStartTime());
			
			emailReminderProducer.sendMessage(therapistAppointment.getClientId(), emailReminderEvent);
			
		}
		*/
	}
}
