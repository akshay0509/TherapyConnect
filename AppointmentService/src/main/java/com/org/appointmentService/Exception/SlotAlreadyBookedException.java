package com.org.appointmentService.Exception;

@SuppressWarnings("serial")
public class SlotAlreadyBookedException extends RuntimeException{

	public SlotAlreadyBookedException() {
        super("The selected slot has already been booked");
    }

    public SlotAlreadyBookedException(String message) {
        super(message);
    }
}
