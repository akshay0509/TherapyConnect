package com.org.notificationService.Meeting;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.org.notificationService.Services.GoogleCalendarService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GoogleConferenceTest {
    @Test void googleSuccessDatabaseRollbackRetryUsesSameEventId() throws Exception {
        var calendar=mock(Calendar.class);var events=mock(Calendar.Events.class);
        var get=mock(Calendar.Events.Get.class);var insert=mock(Calendar.Events.Insert.class,RETURNS_SELF);
        when(calendar.events()).thenReturn(events);when(events.get(eq("primary"),anyString())).thenReturn(get);
        var missing=new GoogleJsonResponseException(new HttpResponseException.Builder(404,"Not found",new HttpHeaders()),null);
        var created=new Event().setId("existing");when(get.execute()).thenThrow(missing).thenReturn(created);
        when(events.insert(eq("primary"),any(Event.class))).thenReturn(insert);when(insert.execute()).thenReturn(created);
        var update=mock(Calendar.Events.Update.class,RETURNS_SELF);
        when(events.update(eq("primary"),anyString(),any(Event.class))).thenReturn(update);when(update.execute()).thenReturn(created);
        var service=new GoogleCalendarService(calendar);var start=LocalDateTime.of(2026,9,28,10,0);
        service.createAppointmentEvent("c@example.test","t@example.test","Session","Appointment ID: APP123",
            start,start.plusHours(1),"ONLINE",null,ZoneId.of("UTC"));
        service.createAppointmentEvent("c@example.test","t@example.test","Session","Appointment ID: APP123",
            start,start.plusHours(1),"ONLINE",null,ZoneId.of("UTC"));
        var ids=ArgumentCaptor.forClass(String.class);verify(events,times(3)).get(eq("primary"),ids.capture());
        assertEquals(ids.getAllValues().get(0),ids.getAllValues().get(1));
        assertTrue(ids.getAllValues().get(0).matches("[a-v0-9]{5,1024}"));
        verify(events,times(1)).insert(eq("primary"),any(Event.class));
    }
    @Test void onlineReschedulePreservesConferenceAndOfflineRemovesIt() throws Exception {
        var calendar=mock(Calendar.class);var events=mock(Calendar.Events.class);var get=mock(Calendar.Events.Get.class);
        var update=mock(Calendar.Events.Update.class,RETURNS_SELF);when(calendar.events()).thenReturn(events);
        when(events.get("primary","g1")).thenReturn(get);when(events.update(eq("primary"),eq("g1"),any(Event.class))).thenReturn(update);
        var conference=new ConferenceData();var existing=new Event().setId("g1").setConferenceData(conference);
        when(get.execute()).thenReturn(existing);when(update.execute()).thenReturn(existing);
        var service=new GoogleCalendarService(calendar);var start=LocalDateTime.of(2026,9,28,10,0);
        service.updateAppointmentEvent("g1","c@example.test","t@example.test","Session","Appointment ID: APP123",
            start,start.plusHours(1),"ONLINE",null,ZoneId.of("UTC"));
        assertSame(conference,existing.getConferenceData());
        service.updateAppointmentEvent("g1","c@example.test","t@example.test","Session","Appointment ID: APP123",
            start,start.plusHours(1),"OFFLINE","Clinic",ZoneId.of("UTC"));
        assertNull(existing.getConferenceData());assertEquals("Clinic",existing.getLocation());
    }
}
