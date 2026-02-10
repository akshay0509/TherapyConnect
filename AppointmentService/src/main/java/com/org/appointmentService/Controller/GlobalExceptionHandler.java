package com.org.appointmentService.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.org.appointmentService.Dto.ApiError;
import com.org.appointmentService.Exception.SlotAlreadyBookedException;

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
}
