package com.org.appointmentService.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.org.appointmentService.Dto.BookAppointmentRequest;
import com.org.appointmentService.Services.AppointmentService;

import jakarta.validation.Valid;

@RestController
public class AppointmentController {

	@Autowired
    private AppointmentService appointmentService;
	
	@PostMapping("/create-appointment")
    public ResponseEntity<String> bookAppointment(@RequestBody @Valid BookAppointmentRequest bookAppointmentRequest) {

        String appointmentId = appointmentService.bookAppointment(bookAppointmentRequest);
        return ResponseEntity.ok(appointmentId);
    }
}
