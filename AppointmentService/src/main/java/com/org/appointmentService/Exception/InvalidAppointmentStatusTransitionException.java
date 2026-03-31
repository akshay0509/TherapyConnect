package com.org.appointmentService.Exception;

@SuppressWarnings("serial")
public class InvalidAppointmentStatusTransitionException extends RuntimeException {

    public InvalidAppointmentStatusTransitionException(String message) {
        super(message);
    }
}
