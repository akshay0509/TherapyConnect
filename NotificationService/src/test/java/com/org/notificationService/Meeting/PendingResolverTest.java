package com.org.notificationService.Meeting;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.Optional;
import com.org.notificationService.Entity.AppointmentMeetingState;
import com.org.notificationService.Repository.AppointmentMeetingStateRepository;
import com.org.notificationService.Services.*;
import org.junit.jupiter.api.Test;
class PendingResolverTest {
    @Test void cancellationTombstonePreventsGoogleRead() throws Exception {
        var repo=mock(AppointmentMeetingStateRepository.class); var google=mock(GoogleCalendarService.class);
        var states=mock(MeetingStateService.class); var s=new AppointmentMeetingState(); s.setMeetingStatus("NOT_APPLICABLE");
        when(repo.lockState("a1")).thenReturn(Optional.of(s)); new PendingMeetingResolver(repo,google,states).resolve("a1");
        verifyNoInteractions(google,states);
    }
    @Test void exhaustedGoogleFailuresBecomeFailedWithoutRecreatingInvite() throws Exception {
        var repo=mock(AppointmentMeetingStateRepository.class); var google=mock(GoogleCalendarService.class);
        var states=mock(MeetingStateService.class); var s=new AppointmentMeetingState(); s.setMeetingStatus("PENDING");
        s.setRetryCount(11); s.setGoogleCalendarEventId("g1"); s.setNextAttemptAt(LocalDateTime.now().minusSeconds(1));
        when(repo.lockState("a1")).thenReturn(Optional.of(s)); when(google.getAppointmentEvent("g1")).thenThrow(new Exception("unavailable"));
        new PendingMeetingResolver(repo,google,states).resolve("a1");
        assertEquals("FAILED",s.getMeetingStatus()); assertNull(s.getNextAttemptAt()); verify(states).save(s);
        verify(google).getAppointmentEvent("g1"); verifyNoMoreInteractions(google);
    }
}
