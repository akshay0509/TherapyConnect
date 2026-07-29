package com.org.clientService.Dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.org.clientService.Entity.ClientIntake;
import com.org.clientService.Entity.ClientIntake.Status;

public record ClientIntakeDto(
        String intakeId,
        String responseId,
        Status status,
        String clientId,
        LocalDateTime submittedAt,
        LocalDateTime receivedAt,
        LocalDateTime reviewedAt,
        String rejectionReason,
        IntakeClientData client,
        Map<String, Object> rawAnswers
) {
    public static ClientIntakeDto from(ClientIntake intake) {
        IntakeClientData client = new IntakeClientData(
                intake.getFullName(), intake.getFirstName(), intake.getLastName(), intake.getDob(),
                intake.getPronouns(), intake.getGender(), intake.getQualification(), intake.getOccupation(),
                intake.getPhoneNumber(), intake.getEmail(), intake.getPreferredDays(), intake.getPreferredTimings(),
                intake.getPreferredModes(), intake.getEmergencyContactName(), intake.getEmergencyContactAge(),
                intake.getEmergencyContactRelationship(), intake.getEmergencyPhoneNumber(), intake.isConsent());
        return new ClientIntakeDto(
                intake.getIntakeId(), intake.getResponseId(), intake.getStatus(),
                intake.getClientId(), intake.getSubmittedAt(), intake.getReceivedAt(), intake.getReviewedAt(),
                intake.getRejectionReason(), client,
                Collections.unmodifiableMap(new LinkedHashMap<>(intake.getRawAnswers())));
    }
}
