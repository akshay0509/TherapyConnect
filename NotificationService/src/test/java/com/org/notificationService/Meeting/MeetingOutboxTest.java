package com.org.notificationService.Meeting;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.notificationService.Entity.*;
import com.org.notificationService.Repository.MeetingOutboxRepository;
import com.org.notificationService.Services.MeetingOutboxService;
import com.org.notificationService.Scheduler.MeetingOutboxScheduler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MeetingOutboxTest {
    @Test void capturesOwnershipRevisionAndMonotonicSequenceWithoutBusinessEvents() throws Exception {
        var repo=mock(MeetingOutboxRepository.class);var mapper=new ObjectMapper();
        var service=new MeetingOutboxService(mapper,repo);
        var state=new AppointmentMeetingState();state.setAppointmentId("a1");state.setTherapistId("t1");
        state.setCalendarRevision(4L);state.setGoogleCalendarEventId("g1");state.setMeetingStatus("READY");
        state.setMeetingUrl("https://meet.google.com/abc-defg-hij");
        service.saveOutboxEvent(state);service.saveOutboxEvent(state);
        var captor=ArgumentCaptor.forClass(MeetingOutboxEvent.class);verify(repo,times(2)).save(captor.capture());
        var first=mapper.readTree(captor.getAllValues().get(0).getPayload());
        var second=mapper.readTree(captor.getAllValues().get(1).getPayload());
        assertEquals("AppointmentMeetingUpdated",first.path("eventType").asText());
        assertEquals(4,first.path("calendarRevision").asLong());assertEquals(1,first.path("meetingSequence").asLong());
        assertEquals(2,second.path("meetingSequence").asLong());assertTrue(first.path("calendarEligible").asBoolean());
        assertNotEquals(first.path("eventId").asText(),second.path("eventId").asText());
    }
    @Test void brokerFailureDoesNotMarkPublishedOrSkipOrdering() {
        var repo=mock(MeetingOutboxRepository.class);var mapper=new ObjectMapper();
        @SuppressWarnings("unchecked") KafkaTemplate<String,JsonNode> kafka=mock(KafkaTemplate.class);
        var first=new MeetingOutboxEvent();first.setEventId("e1");first.setAppointmentId("a1");first.setPayload("{}");
        var second=new MeetingOutboxEvent();second.setEventId("e2");second.setAppointmentId("a1");second.setPayload("{}");
        when(repo.findTop100ByPublishedFalseAndParkedFalseOrderByCreatedAtAsc()).thenReturn(List.of(first,second));
        when(kafka.send(eq("appointment-meeting-events"),eq("a1"),any(JsonNode.class)))
            .thenReturn(CompletableFuture.<SendResult<String,JsonNode>>failedFuture(new RuntimeException("broker unavailable")));
        new MeetingOutboxScheduler(repo,mapper,kafka).publish();
        assertFalse(first.isPublished());assertEquals(1,first.getRetryCount());assertFalse(first.isParked());
        verify(repo).save(first);verify(repo,never()).save(second);
    }
}
