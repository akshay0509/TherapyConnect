package com.org.appointmentService.Exception;

@SuppressWarnings("serial")
public class SlotNotAvailableException extends RuntimeException {

    public SlotNotAvailableException(String slotId) {
        super("Slot is not available: " + slotId);
    }
}
