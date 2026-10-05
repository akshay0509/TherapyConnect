package com.org.appointmentService.Meeting;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.appointmentService.Messaging.Consumer.AppointmentMeetingConsumer;
import com.org.appointmentService.Repository.TherapistAppointmentsRepository;
import com.org.appointmentService.Entity.TherapistAppointments;
import com.org.events.TherapistAppointment.AppointmentMeetingUpdated;
import org.junit.jupiter.api.Test;

class MeetingConsumerTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final TherapistAppointmentsRepository repository=mock(TherapistAppointmentsRepository.class);
    private final AppointmentMeetingConsumer consumer=new AppointmentMeetingConsumer(mapper,repository);
    private AppointmentMeetingUpdated event() {
        var e=new AppointmentMeetingUpdated(); e.setAppointmentId("a1"); e.setTherapistId("t1");
        e.setCalendarRevision(3); e.setMeetingSequence(2); e.setCalendarEligible(true);
        e.setMeetingStatus("READY"); e.setMeetingUrl("https://meet.google.com/abc-defg-hij"); return e;
    }
    @Test void projectsOnlyMeetingFieldsWithRevisionAndOwnershipGuard() {
        consumer.listen(mapper.valueToTree(event()));
        verify(repository).applyMeetingUpdate("a1","t1",3L,2L,"READY","https://meet.google.com/abc-defg-hij",true);
        verifyNoMoreInteractions(repository);
    }
    @Test void nullsStaleUrlForPendingOrFailed() {
        var e=event(); e.setMeetingStatus("PENDING"); consumer.listen(mapper.valueToTree(e));
        verify(repository).applyMeetingUpdate("a1","t1",3L,2L,"PENDING",null,true);
    }
    @Test void rejectsUnsafeDestinationsBeforeDatabaseWrite() {
        var e=event(); e.setMeetingUrl("https://meet.google.com.evil.test/abc-defg-hij");
        assertThrows(IllegalArgumentException.class,()->consumer.listen(mapper.valueToTree(e)));
        verifyNoInteractions(repository);
    }
    @Test void oldBackendColumnsCanBeNullAndRescheduleClearsCachedLink() {
        var a=new TherapistAppointments(); a.setCalendarRevision(null); a.setMeetingUrl("old"); a.setMeetingSequence(99L);
        a.advanceCalendarRevision(true,"ONLINE"); assertEquals(1L,a.getCalendarRevision());
        assertNull(a.getMeetingUrl()); assertEquals("PENDING",a.getMeetingStatus()); assertEquals(0L,a.getMeetingSequence());
        a.advanceCalendarRevision(false,"ONLINE"); assertEquals("NOT_APPLICABLE",a.getMeetingStatus());
        a.advanceCalendarRevision(true,"OFFLINE"); assertEquals("NOT_APPLICABLE",a.getMeetingStatus());
    }
}
