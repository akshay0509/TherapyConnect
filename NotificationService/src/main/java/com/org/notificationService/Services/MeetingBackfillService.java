package com.org.notificationService.Services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import com.org.events.TherapistAppointment.AppointmentEvent;
import com.org.notificationService.Repository.AppointmentCalendarEventRepository;
import com.org.notificationService.Repository.AppointmentMeetingStateRepository;
import com.org.notificationService.Repository.TherapistProjectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingBackfillService {
    private final AppointmentCalendarEventRepository mappings;
    private final AppointmentMeetingStateRepository repository;
    private final TherapistProjectionRepository therapists;
    private final GoogleCalendarService google;
    private final MeetingStateService states;
    public MeetingBackfillService(AppointmentCalendarEventRepository mappings, AppointmentMeetingStateRepository repository,
            TherapistProjectionRepository therapists, GoogleCalendarService google, MeetingStateService states) {
        this.mappings=mappings; this.repository=repository; this.therapists=therapists; this.google=google; this.states=states;
    }
    @Transactional(rollbackFor = Exception.class)
    public String inspect(String appointmentId, String therapistId, long revision, String start, String end, boolean apply) throws Exception {
        if (revision != 0) return "SKIP_VERSIONED"; // backfill never replaces newer feature activity
        var mapping=mappings.findById(appointmentId).orElse(null);
        if (mapping==null) return "SKIP_NO_MAPPING"; // no invitation creation/repair
        var therapist=therapists.findById(therapistId).orElse(null);
        if (therapist==null) return "SKIP_NO_THERAPIST";
        ZoneId zone=ZoneId.of(therapist.getTimezone());
        var event=google.getAppointmentEvent(mapping.getGoogleCalendarEventId());
        if (!Objects.equals(event.getDescription(), "Appointment ID: " + appointmentId)) return "SKIP_ID_MISMATCH";
        if (event.getStart()==null || event.getEnd()==null || event.getStart().getDateTime()==null || event.getEnd().getDateTime()==null)
            return "SKIP_TIME_MISMATCH";
        if (event.getStart().getDateTime().getValue()!=LocalDateTime.parse(start).atZone(zone).toInstant().toEpochMilli()
                || event.getEnd().getDateTime().getValue()!=LocalDateTime.parse(end).atZone(zone).toInstant().toEpochMilli()) return "SKIP_TIME_MISMATCH";
        if ("cancelled".equals(event.getStatus()) || MeetingStateService.meetUrl(event)==null) return "SKIP_NO_LINK";
        if (!apply) return "WOULD_APPLY";
        AppointmentEvent source=new AppointmentEvent(); source.setAppointmentId(appointmentId);
        source.setTherapistId(therapistId); source.setCalendarRevision(0L);
        var state=states.begin(source);
        if (state==null) return "SKIP_NEWER_REVISION";
        if ("READY".equals(state.getMeetingStatus()) && Objects.equals(state.getMeetingUrl(), MeetingStateService.meetUrl(event))) return "ALREADY_READY";
        states.capture(state,event,"ONLINE");
        return "APPLIED";
    }
}
