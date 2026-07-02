package com.org.notificationService.Services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);

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
			LocalDateTime endTime,
			String modeType,
			String address,
			ZoneId zone) throws Exception {

		logger.info("inside createAppointmentEvent..");

		Event event = new Event();

		event.setSummary(summary);
		event.setDescription(description);

		event.setStart(buildEventDateTime(startTime, zone));
		event.setEnd(buildEventDateTime(endTime, zone));
		event.setAttendees(buildAttendees(clientEmail, therapistEmail));

		if ("OFFLINE".equals(modeType)) {
			if (address != null && !address.isBlank()) {
				event.setLocation(address);
			}
		} else {
			event.setConferenceData(buildConferenceData());
		}

		Event createdEvent =
				calendar.events()
				.insert("primary", event)
				.setConferenceDataVersion(1)
				.setSendUpdates("all")
				.execute();

		logger.info("exiting 1..");
		logger.info("calendar event created. eventId={}", createdEvent.getId());
		return createdEvent.getId();
	}

	public void updateAppointmentEvent(
			String googleCalendarEventId,
			String clientEmail,
			String therapistEmail,
			String summary,
			String description,
			LocalDateTime startTime,
			LocalDateTime endTime,
			String modeType,
			String address,
			ZoneId zone) throws Exception {

		Event existingEvent = calendar.events().get("primary", googleCalendarEventId).execute();
		existingEvent.setSummary(summary);
		existingEvent.setDescription(description);
		existingEvent.setStart(buildEventDateTime(startTime, zone));
		existingEvent.setEnd(buildEventDateTime(endTime, zone));
		existingEvent.setAttendees(buildAttendees(clientEmail, therapistEmail));

		if ("OFFLINE".equals(modeType)) {
			existingEvent.setConferenceData(null);
			if (address != null && !address.isBlank()) {
				existingEvent.setLocation(address);
			}
		} else {
			existingEvent.setLocation(null);
		}

		calendar.events()
		.update("primary", googleCalendarEventId, existingEvent)
		.setSendUpdates("all")
		.execute();

		logger.info("calendar event updated. eventId={}", googleCalendarEventId);
	}

	public void cancelAppointmentEvent(String googleCalendarEventId) throws Exception {
		deleteCalendarEvent(googleCalendarEventId, true);
	}

	public String createAvailabilityBlockEvent(
			String summary,
			String description,
			LocalDateTime startTime,
			LocalDateTime endTime,
			ZoneId zone) throws Exception {

		Event event = new Event();
		event.setSummary(summary);
		event.setDescription(description);
		event.setStart(buildEventDateTime(startTime, zone));
		event.setEnd(buildEventDateTime(endTime, zone));

		Event createdEvent = calendar.events()
				.insert("primary", event)
				.setSendUpdates("none")
				.execute();

		logger.info("availability block event created. eventId={}", createdEvent.getId());
		return createdEvent.getId();
	}

	public void deleteCalendarEvent(String googleCalendarEventId, boolean sendUpdates) throws Exception {
		calendar.events()
		.delete("primary", googleCalendarEventId)
		.setSendUpdates(sendUpdates ? "all" : "none")
		.execute();

		logger.info("calendar event deleted. eventId={}", googleCalendarEventId);
	}

	private List<EventAttendee> buildAttendees(String clientEmail, String therapistEmail) {
		EventAttendee client = new EventAttendee().setEmail(clientEmail);
		EventAttendee therapist = new EventAttendee().setEmail(therapistEmail);
		return List.of(client, therapist);
	}

	private ConferenceData buildConferenceData() {
		return new ConferenceData()
				.setCreateRequest(
						new CreateConferenceRequest()
						.setRequestId(UUID.randomUUID().toString())
						.setConferenceSolutionKey(
								new ConferenceSolutionKey().setType("hangoutsMeet")
								)
						);
	}

	private EventDateTime buildEventDateTime(LocalDateTime time, ZoneId zone) {
		return new EventDateTime()
				.setDateTime(convertToDateTime(time, zone))
				.setTimeZone(zone.toString());
	}

	private com.google.api.client.util.DateTime convertToDateTime(LocalDateTime time, ZoneId zone) {
		return new com.google.api.client.util.DateTime(
				time.atZone(zone)
				.toInstant()
				.toEpochMilli()
				);
	}
}
