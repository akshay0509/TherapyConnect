package com.org.therapistService.Scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.org.therapistService.Messaging.EmailReminderProducer;

@Service
public class EmailReminderScheduler {
	
	@Autowired
	EmailReminderProducer emailReminderProducer;

	@Scheduled(cron = "0 0 19 * * *")
	public void publishEmailReminders() {
		emailReminderProducer.sendMessage(null, null);
	}
}
