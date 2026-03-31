package com.org.appointmentService.Exception;

@SuppressWarnings("serial")
public class AppointmentNotFoundException extends RuntimeException {

    public AppointmentNotFoundException(String appointmentId) {
        super("Appointment not found: " + appointmentId);
    }
}
