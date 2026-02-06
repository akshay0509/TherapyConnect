package com.org.notificationService.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.org.events.email.EmailReminderEvent;

@Service
public class EmailSenderService {

	@Autowired
	private JavaMailSender javaMailSender;
	
	public void sendReminderEmail(EmailReminderEvent emailReminderEvent) {
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom("no-reply-therapywithsaipriya@gmail.com");
		mail.setTo(emailReminderEvent.getEmail());
		mail.setSubject("Appointment Reminder | "+emailReminderEvent.getAppointmentTime().toString());
		mail.setText("Your appointment is scheduled for:\nDate: "+emailReminderEvent.getAppointmentTime().toString() + "\nTime: "+emailReminderEvent.getAppointmentTime().toString());
		
		javaMailSender.send(mail);
	}
}
