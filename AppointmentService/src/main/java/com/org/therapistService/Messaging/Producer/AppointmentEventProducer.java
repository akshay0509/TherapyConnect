package com.org.therapistService.Messaging.Producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.org.events.TherapistAppointment.AppointmentEvent;

@Service
public class AppointmentEventProducer {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private static final String APPOINTMENT_EVENT_TOPIC = "therapist-appointment-events";

	public void publishAppointment(AppointmentEvent appointmentEvent) {
		kafkaTemplate.send(APPOINTMENT_EVENT_TOPIC, appointmentEvent.getEventId(), appointmentEvent);
	}

}
