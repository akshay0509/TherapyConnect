package com.org.notificationService.Meeting;
import static org.mockito.Mockito.*;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.calendar.model.Event;
import com.org.events.TherapistAppointment.AppointmentEvent;
import com.org.notificationService.Entity.*;
import com.org.notificationService.Repository.*;
import com.org.notificationService.Services.*;
import com.org.notificationService.Messaging.AppointmentEventConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AppointmentInviteLifecycleTest {
    final ObjectMapper mapper=new ObjectMapper();
    final GoogleCalendarService google=mock(GoogleCalendarService.class);
    final MeetingStateService states=mock(MeetingStateService.class);
    final AppointmentCalendarEventRepository mappings=mock(AppointmentCalendarEventRepository.class);
    final ClientProjectionRepository clients=mock(ClientProjectionRepository.class);
    final TherapistProjectionRepository therapists=mock(TherapistProjectionRepository.class);
    final AppointmentEventConsumer consumer=new AppointmentEventConsumer();
    final AppointmentMeetingState state=new AppointmentMeetingState();
    AppointmentInviteLifecycleTest() {
        ReflectionTestUtils.setField(consumer,"objectMapper",mapper);
        ReflectionTestUtils.setField(consumer,"googleCalendarService",google);
        ReflectionTestUtils.setField(consumer,"meetingStateService",states);
        ReflectionTestUtils.setField(consumer,"appointmentCalendarEventRepository",mappings);
        ReflectionTestUtils.setField(consumer,"clientProjectionRepository",clients);
        ReflectionTestUtils.setField(consumer,"therapistProjectionRepository",therapists);
        ReflectionTestUtils.setField(consumer,"fallbackTherapistEmail","therapist@example.test");
        state.setAppointmentId("a1");
    }
    AppointmentEvent source(String type, boolean eligible, String mode) {
        var event=new AppointmentEvent(); event.setEventType(type); event.setAppointmentId("a1");
        event.setTherapistId("t1"); event.setClientId("c1"); event.setCalendarRevision(2L);
        event.setCalendarEligible(eligible); event.setModeType(mode); return event;
    }
    void existingMapping() {
        var mapping=new AppointmentCalendarEvent(); mapping.setAppointmentId("a1");mapping.setGoogleCalendarEventId("g1");
        when(mappings.findById("a1")).thenReturn(Optional.of(mapping));
        var client=new ClientProjection(); client.setEmail("client@example.test");client.setFirstName("Alex");client.setLastName("Morgan");
        when(clients.findById("c1")).thenReturn(Optional.of(client));
        when(therapists.findById("t1")).thenReturn(Optional.empty());
    }
    @Test void onlineRescheduleCapturesExistingConferenceWithoutCreation() throws Exception {
        existingMapping(); when(states.begin(any())).thenReturn(state);
        var result=new Event().setId("g1").setHangoutLink("https://meet.google.com/abc-defg-hij");
        when(google.updateAppointmentEvent(eq("g1"),any(),any(),any(),any(),any(),any(),eq("ONLINE"),any(),any())).thenReturn(result);
        consumer.listen(mapper.valueToTree(source("AppointmentRescheduled",true,"ONLINE")));
        verify(states).capture(state,result,"ONLINE");
        verify(google,never()).createAppointmentEvent(any(),any(),any(),any(),any(),any(),any(),any(),any());
    }
    @Test void unconfirmedMissingMappingDoesNotCreateAnInvite() throws Exception {
        when(states.begin(any())).thenReturn(state); when(mappings.findById("a1")).thenReturn(Optional.empty());
        consumer.listen(mapper.valueToTree(source("AppointmentRescheduled",false,"ONLINE")));
        verifyNoInteractions(google); verify(states).clear(state);
    }
    @Test void legacyRescheduleWithoutMappingIsNotApplicable() throws Exception {
        var stateRepository=mock(AppointmentMeetingStateRepository.class);
        var outbox=mock(MeetingOutboxService.class);
        when(stateRepository.lockState("a1")).thenReturn(Optional.of(state));
        ReflectionTestUtils.setField(consumer,"meetingStateService",new MeetingStateService(stateRepository,outbox));
        when(mappings.findById("a1")).thenReturn(Optional.empty());
        var event=source("AppointmentRescheduled",false,"ONLINE");
        event.setCalendarEligible(null);
        consumer.listen(mapper.valueToTree(event));
        verifyNoInteractions(google);
        org.junit.jupiter.api.Assertions.assertEquals("NOT_APPLICABLE",state.getMeetingStatus());
        org.junit.jupiter.api.Assertions.assertNull(state.getMeetingUrl());
        verify(outbox).saveOutboxEvent(state);
        verify(stateRepository).save(state);
    }
    @Test void confirmedMissingMappingRequiresReviewInsteadOfDuplicatingInvitation() throws Exception {
        when(states.begin(any())).thenReturn(state); when(mappings.findById("a1")).thenReturn(Optional.empty());
        consumer.listen(mapper.valueToTree(source("AppointmentRescheduled",true,"ONLINE")));
        verifyNoInteractions(google); verify(states).save(state);
        org.junit.jupiter.api.Assertions.assertEquals("FAILED",state.getMeetingStatus());
    }
    @Test void staleLifecycleEventMakesNoGoogleChanges() throws Exception {
        when(states.begin(any())).thenReturn(null);
        consumer.listen(mapper.valueToTree(source("AppointmentRescheduled",true,"ONLINE")));
        verifyNoInteractions(google,mappings,clients);
    }
    @Test void cancellationClearsProjectionAndRetainsStateTombstone() throws Exception {
        var mapping=new AppointmentCalendarEvent();mapping.setGoogleCalendarEventId("g1");
        when(mappings.findById("a1")).thenReturn(Optional.of(mapping)); when(states.begin(any())).thenReturn(state);
        consumer.listen(mapper.valueToTree(source("AppointmentCancelled",false,"ONLINE")));
        verify(google).cancelAppointmentEvent("g1");verify(mappings).delete(mapping);verify(states).clear(state);
    }
    @Test void newInviteOmitsMissingLastName() throws Exception {
        when(states.begin(any())).thenReturn(state);
        when(mappings.findById("a1")).thenReturn(Optional.empty());
        var client=new ClientProjection(); client.setEmail("client@example.test"); client.setFirstName("Alex");
        when(clients.findById("c1")).thenReturn(Optional.of(client));
        when(therapists.findById("t1")).thenReturn(Optional.empty());
        when(google.createAppointmentEvent(any(),any(),any(),any(),any(),any(),any(),any(),any()))
                .thenReturn(new Event().setId("g1"));
        consumer.listen(mapper.valueToTree(source("AppointmentConfirmed",true,"ONLINE")));
        verify(google).createAppointmentEvent(
                eq("client@example.test"),eq("therapist@example.test"),eq("Therapy Session with Alex"),
                eq("Appointment ID: a1"),isNull(),isNull(),eq("ONLINE"),isNull(),any());
    }
    @Test void rescheduledInviteOmitsMissingLastName() throws Exception {
        existingMapping(); clients.findById("c1").orElseThrow().setLastName(null);
        when(states.begin(any())).thenReturn(state);
        when(google.updateAppointmentEvent(any(),any(),any(),any(),any(),any(),any(),any(),any(),any()))
                .thenReturn(new Event().setId("g1"));
        consumer.listen(mapper.valueToTree(source("AppointmentRescheduled",true,"ONLINE")));
        verify(google).updateAppointmentEvent(
                eq("g1"),eq("client@example.test"),eq("therapist@example.test"),
                eq("Therapy Session with Alex"),eq("Appointment ID: a1"),
                isNull(),isNull(),eq("ONLINE"),isNull(),any());
    }}