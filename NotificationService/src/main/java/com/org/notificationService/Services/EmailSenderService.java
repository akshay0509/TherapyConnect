package com.org.notificationService.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.org.notificationService.Dto.ClientReminderDto;

@Service
public class EmailSenderService {

	@Autowired
	private JavaMailSender javaMailSender;
	
	public void sendReminderEmail(ClientReminderDto clientReminderDto) {
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom("no-reply-therapywithsaipriya@gmail.com");
		mail.setTo(clientReminderDto.getEmail());
		mail.setSubject("Appointment Reminder | "+clientReminderDto.getAppointmentDate().toString());
		mail.setText("Your appointment is scheduled for:\nDate: "+clientReminderDto.getAppointmentDate().toString() + "\nTime: "+clientReminderDto.getAppointmentTime().toString());
		
		javaMailSender.send(mail);
	}
}
