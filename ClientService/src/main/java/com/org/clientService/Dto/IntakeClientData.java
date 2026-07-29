package com.org.clientService.Dto;

import java.sql.Date;

public record IntakeClientData(
        String fullName,
        String firstName,
        String lastName,
        Date dob,
        String pronouns,
        String gender,
        String qualification,
        String occupation,
        String phoneNumber,
        String email,
        String preferredDays,
        String preferredTimings,
        String preferredModes,
        String emergencyContactName,
        Integer emergencyContactAge,
        String emergencyContactRelationship,
        String emergencyPhoneNumber,
        boolean consent
) {}
