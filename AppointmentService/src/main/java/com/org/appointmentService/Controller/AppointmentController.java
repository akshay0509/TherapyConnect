package com.org.appointmentService.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.appointmentService.Dto.AvailabilityResponseDto;
import com.org.appointmentService.Dto.BookAppointmentRequest;
import com.org.appointmentService.Entity.TherapistAppointments;
import com.org.appointmentService.Services.AppointmentService;
import com.org.appointmentService.Utility.SecurityUtils;

import jakarta.validation.Valid;

@RestController
public class AppointmentController {

	@Autowired
    private AppointmentService appointmentService;
	
	@PostMapping("/create-appointment")
    public ResponseEntity<String> bookAppointment(@RequestBody @Valid BookAppointmentRequest bookAppointmentRequest) {

        String appointmentId;
		try {
			appointmentId = appointmentService.bookAppointment(bookAppointmentRequest);
		} catch (JsonProcessingException e) {			
			return ResponseEntity.ok("failed");
		}
        return ResponseEntity.ok(appointmentId);
    }
	
	@GetMapping("/get-appointments")
    public ResponseEntity<List<TherapistAppointments>> getAppointment() {

		String therapistId = SecurityUtils.getTherapistId();
		List<TherapistAppointments> therapistAppointmentsList = appointmentService.getTherapistAppointments(therapistId);
        return ResponseEntity.ok(therapistAppointmentsList);
    }
	
	@GetMapping("/get-availability")
	public ResponseEntity<List<AvailabilityResponseDto>> getTherapistAvailability(){
		
		String therapistId = SecurityUtils.getTherapistId();
		List<AvailabilityResponseDto> availabilityResponseDtoList = appointmentService.getTherapistAvailabilityWithAppointments(therapistId);
		return ResponseEntity.ok(availabilityResponseDtoList);
	}
}
