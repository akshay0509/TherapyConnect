package com.org.appointmentService.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.appointmentService.Dto.AppointmentScheduleAppointmentDto;
import com.org.appointmentService.Dto.AppointmentScheduleViewDto;
import com.org.appointmentService.Dto.AvailabilityResponseDto;
import com.org.appointmentService.Dto.BookAppointmentRequest;
import com.org.appointmentService.Dto.RescheduleAppointmentRequest;
import com.org.appointmentService.Dto.UpdateAppointmentStatusRequest;
import com.org.appointmentService.Entity.TherapistAppointments;
import com.org.appointmentService.Services.AppointmentService;
import com.org.appointmentService.Utility.SecurityUtils;
import com.org.events.TherapistAppointment.AppointmentStatus;

import jakarta.validation.Valid;

@RestController
public class AppointmentController {

	@Autowired
	private AppointmentService appointmentService;

	@PostMapping("/create-appointment")
	public ResponseEntity<String> bookAppointment(@RequestBody @Valid BookAppointmentRequest bookAppointmentRequest) {

		String therapistId = SecurityUtils.getTherapistId();
		bookAppointmentRequest.setTherapistId(therapistId);

		String appointmentId;
		try {
			appointmentId = appointmentService.bookAppointment(bookAppointmentRequest);
		}
		catch (JsonProcessingException e) {			
			return ResponseEntity.ok("failed");
		}
		return ResponseEntity.ok(appointmentId);
	}

	@PatchMapping("/update-appointment")
	public ResponseEntity<String> updateAppointmentStatus(@RequestBody @Valid UpdateAppointmentStatusRequest updateAppointmentStatusRequest) {

		String therapistId = SecurityUtils.getTherapistId();
		updateAppointmentStatusRequest.setTherapistId(therapistId);

		try {
			appointmentService.updateAppointmentStatus(updateAppointmentStatusRequest);
		}
		catch (JsonProcessingException e) {
			return ResponseEntity.internalServerError().body("failed");
		}

		return ResponseEntity.ok("Appointment status updated");
	}

	@PatchMapping("/reschedule-appointment")
	public ResponseEntity<String> rescheduleAppointment(@RequestBody @Valid RescheduleAppointmentRequest rescheduleAppointmentRequest) {

		String therapistId = SecurityUtils.getTherapistId();
		rescheduleAppointmentRequest.setTherapistId(therapistId);

		try {
			appointmentService.rescheduleAppointment(rescheduleAppointmentRequest);
		}
		catch (JsonProcessingException e) {
			return ResponseEntity.internalServerError().body("failed");
		}

		return ResponseEntity.ok("Appointment rescheduled");
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
	
	@GetMapping("/editor-view")
	public ResponseEntity<AppointmentScheduleViewDto> getAppointmentEditorView(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

		if (toDate.isBefore(fromDate)) {
			return ResponseEntity.badRequest().build();
		}

		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(appointmentService.getAppointmentEditorView(therapistId, fromDate, toDate));
	}

	@GetMapping("/search")
	public ResponseEntity<List<AppointmentScheduleAppointmentDto>> searchAppointments(
			@RequestParam(required = false) String clientName,
			@RequestParam(required = false) List<AppointmentStatus> status,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

		if (toDate.isBefore(fromDate)) {
			return ResponseEntity.badRequest().build();
		}

		String therapistId = SecurityUtils.getTherapistId();
		return ResponseEntity.ok(appointmentService.searchAppointments(therapistId, clientName, status, fromDate, toDate));
	}
}
