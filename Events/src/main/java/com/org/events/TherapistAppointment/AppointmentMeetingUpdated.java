package com.org.events.TherapistAppointment;

import lombok.Data;

@Data
public class AppointmentMeetingUpdated {
    private String eventId;
    private String eventType = "AppointmentMeetingUpdated";
    private String appointmentId;
    private String therapistId;
    private long calendarRevision;
    private long meetingSequence;
    private boolean calendarEligible;
    private String meetingStatus;
    private String meetingUrl;
}
