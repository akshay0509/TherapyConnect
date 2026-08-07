package com.org.clientService.Dto;

import java.math.BigDecimal;
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
        boolean consent,

        /* Set by the therapist at approval, not by the client — the intake form
           has no item for either, and pricing is not the client's to state.
           Kept on this record rather than on ClientIntake so they are supplied
           once, at the moment of the decision, instead of being stored as a
           pending property of a submission that may yet be rejected. */
        BigDecimal sessionFee,
        Boolean dsf
) {}
