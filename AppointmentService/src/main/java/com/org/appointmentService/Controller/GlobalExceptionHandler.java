package com.org.appointmentService.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.org.appointmentService.Dto.ApiError;
import com.org.appointmentService.Exception.AppointmentNotFoundException;
import com.org.appointmentService.Exception.InvalidAppointmentStatusTransitionException;
import com.org.appointmentService.Exception.SlotAlreadyBookedException;
import com.org.appointmentService.Exception.SlotNotAvailableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(SlotAlreadyBookedException.class)
    public ResponseEntity<ApiError> handleSlotAlreadyBooked(
            SlotAlreadyBookedException ex) {

		ApiError apiError = new ApiError("SLOT_ALREADY_BOOKED", ex.getMessage());
		return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(apiError);
    }
	
	@ExceptionHandler(SlotNotAvailableException.class)
    public ResponseEntity<ApiError> handleSlotNotAvailable(
            SlotNotAvailableException ex) {

        ApiError apiError = new ApiError("SLOT_NOT_AVAILABLE", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(apiError);
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<ApiError> handleAppointmentNotFound(
            AppointmentNotFoundException ex) {

        ApiError apiError = new ApiError("APPOINTMENT_NOT_FOUND", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(apiError);
    }

    @ExceptionHandler(InvalidAppointmentStatusTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidTransition(
            InvalidAppointmentStatusTransitionException ex) {

        ApiError apiError = new ApiError("INVALID_APPOINTMENT_STATUS_TRANSITION", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiError);
    }
}
