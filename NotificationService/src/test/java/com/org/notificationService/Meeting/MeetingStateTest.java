package com.org.notificationService.Meeting;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import com.google.api.services.calendar.model.*;
import com.org.events.TherapistAppointment.AppointmentEvent;
import com.org.notificationService.Entity.AppointmentMeetingState;
import com.org.notificationService.Repository.AppointmentMeetingStateRepository;
import com.org.notificationService.Services.*;
import org.junit.jupiter.api.Test;

class MeetingStateTest {
    private final AppointmentMeetingStateRepository repository=mock(AppointmentMeetingStateRepository.class);
    private final MeetingOutboxService outbox=mock(MeetingOutboxService.class);
    private final MeetingStateService service=new MeetingStateService(repository,outbox);
    @Test void ignoresOldAndDuplicateRevisionsIncludingLegacyAfterUpgrade() {
        var state=new AppointmentMeetingState(); state.setAppointmentId("a1"); state.setCalendarRevision(4L);
        when(repository.lockState("a1")).thenReturn(Optional.of(state));
        var event=new AppointmentEvent(); event.setAppointmentId("a1"); event.setCalendarRevision(3L);
        assertNull(service.begin(event)); event.setCalendarRevision(4L); assertNull(service.begin(event));
        event.setCalendarRevision(null); assertNull(service.begin(event)); verifyNoInteractions(outbox);
    }
    @Test void pendingCapturePersistsRetryAndOutboxTogether() throws Exception {
        var state=new AppointmentMeetingState(); state.setAppointmentId("a1");
        service.capture(state,new Event().setId("g1"),"ONLINE");
        assertEquals("PENDING",state.getMeetingStatus()); assertNotNull(state.getNextAttemptAt());
        verify(outbox).saveOutboxEvent(state); verify(repository).save(state);
    }
    @Test void offlineAndCancellationClearLinksAndPendingWork() throws Exception {
        var state=new AppointmentMeetingState(); state.setMeetingUrl("https://meet.google.com/abc-defg-hij");
        service.capture(state,new Event().setId("g1").setHangoutLink(state.getMeetingUrl()),"OFFLINE");
        assertNull(state.getMeetingUrl()); assertNull(state.getNextAttemptAt()); assertEquals("NOT_APPLICABLE",state.getMeetingStatus());
        service.clear(state); assertEquals("NOT_APPLICABLE",state.getMeetingStatus());
    }
    @Test void validatesConferenceVideoEntryPoint() {
        var event=new Event().setConferenceData(new ConferenceData().setEntryPoints(java.util.List.of(
            new EntryPoint().setEntryPointType("video").setUri("https://meet.google.com/abc-defg-hij"))));
        assertEquals("https://meet.google.com/abc-defg-hij",MeetingStateService.meetUrl(event));
        event.setHangoutLink("https://evil.test"); event.setConferenceData(null); assertNull(MeetingStateService.meetUrl(event));
    }
    @Test void failedGoogleConferenceIsNotRetriedForever() throws Exception {
        var state=new AppointmentMeetingState();
        var event=new Event().setId("g1").setConferenceData(new ConferenceData().setCreateRequest(
            new CreateConferenceRequest().setStatus(new ConferenceRequestStatus().setStatusCode("failure"))));
        service.capture(state,event,"ONLINE"); assertEquals("FAILED",state.getMeetingStatus()); assertNull(state.getNextAttemptAt());
    }
}
