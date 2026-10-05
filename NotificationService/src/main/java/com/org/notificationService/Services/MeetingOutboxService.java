package com.org.notificationService.Services;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.events.TherapistAppointment.AppointmentMeetingUpdated;
import com.org.notificationService.Entity.AppointmentMeetingState;
import com.org.notificationService.Entity.MeetingOutboxEvent;
import com.org.notificationService.Repository.MeetingOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingOutboxService {
    private final ObjectMapper mapper;
    private final MeetingOutboxRepository repository;
    public MeetingOutboxService(ObjectMapper mapper, MeetingOutboxRepository repository) {
        this.mapper = mapper; this.repository = repository;
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveOutboxEvent(AppointmentMeetingState state) throws Exception {
        state.setMeetingSequence(state.getMeetingSequence() + 1);
        AppointmentMeetingUpdated payload = new AppointmentMeetingUpdated();
        payload.setEventId(UUID.randomUUID().toString());
        payload.setAppointmentId(state.getAppointmentId());
        payload.setTherapistId(state.getTherapistId());
        payload.setCalendarRevision(state.getCalendarRevision());
        payload.setMeetingSequence(state.getMeetingSequence());
        payload.setCalendarEligible(state.getGoogleCalendarEventId() != null);
        payload.setMeetingStatus(state.getMeetingStatus());
        payload.setMeetingUrl(state.getMeetingUrl());
        MeetingOutboxEvent event = new MeetingOutboxEvent();
        event.setEventId(payload.getEventId()); event.setAppointmentId(state.getAppointmentId());
        event.setCreatedAt(LocalDateTime.now()); event.setPayload(mapper.writeValueAsString(payload));
        repository.save(event);
    }
}
