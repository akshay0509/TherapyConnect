package com.org.appointmentService.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.org.appointmentService.Dto.BookAppointmentRequest;
import com.org.appointmentService.Entity.TherapistAppointments;
import com.org.appointmentService.Entity.TherapistAvailability;
import com.org.appointmentService.Enums.AppointmentStatus;
import com.org.appointmentService.Exception.SlotAlreadyBookedException;
import com.org.appointmentService.Repository.TherapistAppointmentsRepository;
import com.org.appointmentService.Repository.TherapistAvailabilityRepository;
import com.org.events.TherapistAppointment.AppointmentEvent;
import com.org.therapistService.Messaging.Producer.AppointmentEventProducer;

import jakarta.transaction.Transactional;

@Service
public class AppointmentService {

	@Autowired
	private TherapistAvailabilityRepository therapistAvailabilityRepository;

	@Autowired
	private TherapistAppointmentsRepository therapistAppointmentsRepository;

	@Autowired
	private AppointmentEventProducer appointmentEventProducer;

	@Transactional
	public String bookAppointment(BookAppointmentRequest bookAppointmentRequest) {

		String slotId = bookAppointmentRequest.getSlotId();

		int updated = therapistAvailabilityRepository.markSlotAsBooked(slotId);
		if (updated == 0) {
			throw new SlotAlreadyBookedException();
		}

		TherapistAvailability therapistAvailability = therapistAvailabilityRepository.findBySlotId(slotId);

		TherapistAppointments therapistAppointment = new TherapistAppointments();

		therapistAppointment.setSlotId(slotId);
		therapistAppointment.setTherapistId(bookAppointmentRequest.getTherapistId());
		therapistAppointment.setClientId(bookAppointmentRequest.getClientId());
		therapistAppointment.setStartTime(therapistAvailability.getStartTime());
		therapistAppointment.setEndTime(therapistAvailability.getEndTime());
		therapistAppointment.setStatus(AppointmentStatus.CONFIRMED);

		therapistAppointmentsRepository.save(therapistAppointment);

		AppointmentEvent appointmentEvent = new AppointmentEvent();
		appointmentEvent.setEventType("AppointmentCreated");
		appointmentEvent.setAppointmentId(therapistAppointment.getAppointmentId());
		appointmentEvent.setSlotId(slotId);
		appointmentEvent.setTherapistId(therapistAppointment.getTherapistId());
		appointmentEvent.setClientId(therapistAppointment.getClientId());
		appointmentEvent.setStartTime(therapistAppointment.getStartTime());
		appointmentEvent.setEndTime(therapistAppointment.getEndTime());
		appointmentEvent.setBookingSource("THERAPIST");

		appointmentEventProducer.publishAppointment(appointmentEvent);

		return therapistAppointment.getAppointmentId();

	}
}
