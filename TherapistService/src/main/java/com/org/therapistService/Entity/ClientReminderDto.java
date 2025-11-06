package com.org.therapistService.Entity;

import java.time.LocalDateTime;

public record ClientReminderDto(String email, LocalDateTime AppointmentTime) {}
