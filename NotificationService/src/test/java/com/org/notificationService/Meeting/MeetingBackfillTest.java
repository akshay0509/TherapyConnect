package com.org.notificationService.Meeting;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import com.org.notificationService.Repository.*;
import com.org.notificationService.Services.*;
import org.junit.jupiter.api.Test;
class MeetingBackfillTest {
    final AppointmentCalendarEventRepository mappings=mock(AppointmentCalendarEventRepository.class);
    final AppointmentMeetingStateRepository repository=mock(AppointmentMeetingStateRepository.class);
    final TherapistProjectionRepository therapists=mock(TherapistProjectionRepository.class);
    final GoogleCalendarService google=mock(GoogleCalendarService.class);
    final MeetingStateService states=mock(MeetingStateService.class);
    final MeetingBackfillService backfill=new MeetingBackfillService(mappings,repository,therapists,google,states);
    @Test void newerRowsAreNeverOverwritten() throws Exception {
        assertEquals("SKIP_VERSIONED",backfill.inspect("a1","t1",1,"","",true));
        verifyNoInteractions(mappings,google,states);
    }
    @Test void missingMappingsNeverGenerateInvites() throws Exception {
        when(mappings.findById("a1")).thenReturn(Optional.empty());
        assertEquals("SKIP_NO_MAPPING",backfill.inspect("a1","t1",0,"","",true));
        verifyNoInteractions(google,states);
    }
}
