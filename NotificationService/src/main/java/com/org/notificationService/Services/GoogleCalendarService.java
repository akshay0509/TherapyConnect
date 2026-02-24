package com.org.notificationService.Services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;

@Service
public class GoogleCalendarService {

	private final Calendar calendar;

    public GoogleCalendarService(Calendar calendar) {
        this.calendar = calendar;
    }

    public String createAppointmentEvent(
            String clientEmail,
            String therapistEmail,
            String summary,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime) throws Exception {

        Event event = new Event();

        event.setSummary(summary);
        event.setDescription(description);

        EventDateTime start = new EventDateTime()
                .setDateTime(convertToDateTime(startTime))
                .setTimeZone(ZoneId.systemDefault().toString());

        EventDateTime end = new EventDateTime()
                .setDateTime(convertToDateTime(endTime))
                .setTimeZone(ZoneId.systemDefault().toString());

        event.setStart(start);
        event.setEnd(end);

        // Add attendees
        EventAttendee client =
                new EventAttendee().setEmail(clientEmail);

        EventAttendee therapist =
                new EventAttendee().setEmail(therapistEmail);

        event.setAttendees(List.of(client, therapist));

        // Generate Google Meet link automatically
        ConferenceData conferenceData =
                new ConferenceData()
                        .setCreateRequest(
                                new CreateConferenceRequest()
                                        .setRequestId(UUID.randomUUID().toString())
                                        .setConferenceSolutionKey(
                                                new ConferenceSolutionKey()
                                                        .setType("hangoutsMeet")
                                        )
                        );

        event.setConferenceData(conferenceData);

        Event createdEvent =
                calendar.events()
                        .insert("primary", event)
                        .setConferenceDataVersion(1)
                        .setSendUpdates("all")
                        .execute();

        return createdEvent.getHangoutLink();
    }

    private com.google.api.client.util.DateTime convertToDateTime(LocalDateTime time) {

        return new com.google.api.client.util.DateTime(
                time.atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
        );
    }
}
