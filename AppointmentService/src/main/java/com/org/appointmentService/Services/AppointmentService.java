package com.org.appointmentService.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.org.appointmentService.Dto.AvailabilityResponseDto;
import com.org.appointmentService.Dto.BookAppointmentRequest;
import com.org.appointmentService.Entity.TherapistAppointments;
import com.org.appointmentService.Entity.TherapistAvailability;
import com.org.appointmentService.Enums.AppointmentStatus;
import com.org.appointmentService.Exception.SlotAlreadyBookedException;
import com.org.appointmentService.Messaging.Producer.AppointmentEventProducer;
import com.org.appointmentService.Repository.TherapistAppointmentsRepository;
import com.org.appointmentService.Repository.TherapistAvailabilityRepository;
import com.org.events.TherapistAppointment.AppointmentEvent;

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
		therapistAppointment.setClientName(bookAppointmentRequest.getClientName());
		therapistAppointment.setSessionType(bookAppointmentRequest.getSessionType());
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
		appointmentEvent.setSessionType(therapistAppointment.getSessionType().toString());
		appointmentEvent.setStartTime(therapistAppointment.getStartTime());
		appointmentEvent.setEndTime(therapistAppointment.getEndTime());
		appointmentEvent.setBookingSource("THERAPIST");

		appointmentEventProducer.publishAppointment(appointmentEvent);

		return therapistAppointment.getAppointmentId();

	}
	
	public List<TherapistAppointments> getTherapistAppointments(String therapistId){
		
		LocalDate today = LocalDate.now();

		LocalDateTime startOfDay = today.atStartOfDay();
		LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
		
		return therapistAppointmentsRepository.findByTherapistIdAndStartTimeBetweenOrderByStartTimeAsc(therapistId, startOfDay, endOfDay);
	}
	
	public List<AvailabilityResponseDto> getTherapistAvailabilityWithAppointments(String therapistId){
		
		return therapistAvailabilityRepository.findSlotsWithAppointment(therapistId);
	}
	
}
