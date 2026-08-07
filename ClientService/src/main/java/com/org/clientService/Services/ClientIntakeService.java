package com.org.clientService.Services;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.clientService.Dto.ApproveIntakeRequest;
import com.org.clientService.Dto.ClientDto;
import com.org.clientService.Dto.ClientIntakeDto;
import com.org.clientService.Dto.GoogleFormSubmission;
import com.org.clientService.Dto.IntakeClientData;
import com.org.clientService.Dto.IntakeDecisionResponse;
import com.org.clientService.Dto.RejectIntakeRequest;
import com.org.clientService.Entity.ClientIntake;
import com.org.clientService.Entity.ClientIntake.Status;
import com.org.clientService.Repository.ClientIntakeRepository;

import jakarta.transaction.Transactional;

@Service
public class ClientIntakeService {

    private final ClientIntakeRepository intakeRepository;
    private final ClientService clientService;
    private final String configuredIntegrationId;
    private final String configuredTherapistId;

    public ClientIntakeService(
            ClientIntakeRepository intakeRepository,
            ClientService clientService,
            @Value("${integration.google-forms.id:}") String configuredIntegrationId,
            @Value("${integration.google-forms.therapist-id:}") String configuredTherapistId) {
        this.intakeRepository = intakeRepository;
        this.clientService = clientService;
        this.configuredIntegrationId = configuredIntegrationId;
        this.configuredTherapistId = configuredTherapistId;
    }

    @Transactional
    public ClientIntakeDto stage(String integrationId, GoogleFormSubmission submission) {
        if (!configuredIntegrationId.equals(integrationId) || configuredTherapistId.isBlank()) {
            throw new IllegalArgumentException("Unknown Google Forms integration.");
        }
        if (submission.responseId() == null || submission.responseId().isBlank()) {
            throw new IllegalArgumentException("responseId is required.");
        }

        return intakeRepository.findByIntegrationIdAndResponseId(integrationId, submission.responseId())
                .map(ClientIntakeDto::from)
                .orElseGet(() -> {
                    ClientIntake intake = mapSubmission(integrationId, submission);
                    if (!intake.isConsent()) {
                        throw new IllegalArgumentException("Consent must be accepted before an intake can be staged.");
                    }
                    return ClientIntakeDto.from(intakeRepository.save(intake));
                });
    }

    public List<ClientIntakeDto> list(String therapistId, Status status) {
        Status resolved = status == null ? Status.PENDING : status;
        return intakeRepository.findByTherapistIdAndStatusOrderBySubmittedAtDesc(therapistId, resolved)
                .stream().map(ClientIntakeDto::from).toList();
    }

    public ClientIntakeDto get(String therapistId, String intakeId) {
        return ClientIntakeDto.from(requireScoped(therapistId, intakeId));
    }

    @Transactional
    public IntakeDecisionResponse approve(
            String therapistId, String reviewerId, String intakeId, ApproveIntakeRequest request)
            throws JsonProcessingException {
        ClientIntake intake = requireScoped(therapistId, intakeId);
        if (intake.getStatus() == Status.APPROVED) {
            return response(intake);
        }
        requirePending(intake);

        IntakeClientData data = request == null || request.client() == null
                ? ClientIntakeDto.from(intake).client()
                : request.client();
        validateForCreation(data);
        applyEditedData(intake, data);
        ClientDto client = toClientDto(therapistId, data);
        intake.setClientId(clientService.createClient(client));

        intake.setStatus(Status.APPROVED);
        intake.setReviewedAt(LocalDateTime.now());
        intake.setReviewedBy(reviewerId);
        intakeRepository.save(intake);
        return response(intake);
    }

    @Transactional
    public IntakeDecisionResponse reject(
            String therapistId, String reviewerId, String intakeId, RejectIntakeRequest request) {
        ClientIntake intake = requireScoped(therapistId, intakeId);
        requirePending(intake);
        intake.setStatus(Status.REJECTED);
        intake.setRejectionReason(request == null ? null : clean(request.reason()));
        intake.setReviewedAt(LocalDateTime.now());
        intake.setReviewedBy(reviewerId);
        intakeRepository.save(intake);
        return response(intake);
    }

    private ClientIntake mapSubmission(String integrationId, GoogleFormSubmission submission) {
        Map<String, Object> a = submission.answers();
        ClientIntake intake = new ClientIntake();
        intake.setIntegrationId(integrationId);
        intake.setResponseId(submission.responseId());
        intake.setTherapistId(configuredTherapistId);
        intake.setSubmittedAt(submission.submittedAt());
        intake.setFullName(text(a, "fullName"));
        intake.setFirstName(text(a, "firstName"));
        intake.setLastName(text(a, "lastName"));
        intake.setDob(date(a, "dob"));
        intake.setPronouns(text(a, "pronouns"));
        intake.setGender(text(a, "gender"));
        intake.setQualification(text(a, "qualification"));
        intake.setOccupation(text(a, "occupation"));
        intake.setPhoneNumber(text(a, "phoneNumber"));
        intake.setEmail(text(a, "email"));
        intake.setPreferredDays(text(a, "preferredDays"));
        intake.setPreferredTimings(text(a, "preferredTimings"));
        intake.setPreferredModes(text(a, "preferredModes"));
        intake.setEmergencyContactName(text(a, "emergencyContactName"));
        intake.setEmergencyContactAge(integer(a, "emergencyContactAge"));
        intake.setEmergencyContactRelationship(text(a, "emergencyContactRelationship"));
        intake.setEmergencyPhoneNumber(text(a, "emergencyPhoneNumber"));
        intake.setConsent(bool(a, "consent"));
        intake.setRawAnswers(a);
        return intake;
    }

    private ClientIntake requireScoped(String therapistId, String intakeId) {
        return intakeRepository.findByTherapistIdAndIntakeId(therapistId, intakeId)
                .orElseThrow(() -> new IllegalArgumentException("Intake not found."));
    }

    private void requirePending(ClientIntake intake) {
        if (intake.getStatus() != Status.PENDING) {
            throw new IllegalStateException("Only pending intakes can be reviewed.");
        }
    }

    private void validateForCreation(IntakeClientData data) {
        if (data == null || clean(data.fullName()) == null || data.dob() == null) {
            throw new IllegalArgumentException("Full name and date of birth are required.");
        }
        if (!data.consent()) {
            throw new IllegalArgumentException("Consent must remain accepted.");
        }
    }

    private ClientDto toClientDto(String therapistId, IntakeClientData data) {
        ClientDto dto = new ClientDto();
        dto.setTherapistId(therapistId);
        dto.setFullName(clean(data.fullName()));
        dto.setFirstName(clean(data.firstName()));
        dto.setLastName(clean(data.lastName()));
        dto.setDob(data.dob());
        dto.setPronouns(clean(data.pronouns()));
        dto.setGender(clean(data.gender()));
        dto.setQualification(clean(data.qualification()));
        dto.setCurrentOccupation(clean(data.occupation()));
        dto.setPhoneNumber(clean(data.phoneNumber()));
        dto.setEmail(clean(data.email()));
        dto.setPreferredDays(clean(data.preferredDays()));
        dto.setPreferredTimings(clean(data.preferredTimings()));
        dto.setPreferredModes(clean(data.preferredModes()));
        dto.setEmergencyContactName(clean(data.emergencyContactName()));
        dto.setEmergencyContactAge(data.emergencyContactAge());
        dto.setEmergencyContactRelationship(clean(data.emergencyContactRelationship()));
        dto.setEmergencyPhoneNumber(clean(data.emergencyPhoneNumber()));
        dto.setSource("GOOGLE_FORM");
        return dto;
    }

    private void applyEditedData(ClientIntake i, IntakeClientData d) {
        i.setFullName(clean(d.fullName()));
        i.setFirstName(clean(d.firstName()));
        i.setLastName(clean(d.lastName()));
        i.setDob(d.dob());
        i.setPronouns(clean(d.pronouns()));
        i.setGender(clean(d.gender()));
        i.setQualification(clean(d.qualification()));
        i.setOccupation(clean(d.occupation()));
        i.setPhoneNumber(clean(d.phoneNumber()));
        i.setEmail(clean(d.email()));
        i.setPreferredDays(clean(d.preferredDays()));
        i.setPreferredTimings(clean(d.preferredTimings()));
        i.setPreferredModes(clean(d.preferredModes()));
        i.setEmergencyContactName(clean(d.emergencyContactName()));
        i.setEmergencyContactAge(d.emergencyContactAge());
        i.setEmergencyContactRelationship(clean(d.emergencyContactRelationship()));
        i.setEmergencyPhoneNumber(clean(d.emergencyPhoneNumber()));
        i.setConsent(d.consent());
    }

    private IntakeDecisionResponse response(ClientIntake intake) {
        return new IntakeDecisionResponse(
                intake.getIntakeId(), intake.getStatus(), intake.getClientId());
    }

    private String text(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof List<?> list) {
            return clean(String.join(", ", list.stream().map(String::valueOf).toList()));
        }
        return value == null ? null : clean(String.valueOf(value));
    }

    /**
     * Google Forms sends whatever the respondent's locale produced, so a date
     * item commonly arrives as "29/03/1983" rather than ISO. Rejecting those
     * meant the submission returned 400, Apps Script marked the row FAILED, and
     * a prospective client was never seen by the therapist.
     *
     * Ambiguity is resolved deliberately, not guessed: if either component is
     * greater than 12 the order is certain, and only when BOTH could be a month
     * does it fall back to day-first, matching the en-IN convention the rest of
     * the app formats with. A genuinely impossible date still throws — failing
     * loudly is right when the alternative is storing the wrong birthday.
     */
    private static final java.util.regex.Pattern SLASH_OR_DASH_DATE =
            java.util.regex.Pattern.compile("^(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})$");

    private Date date(Map<String, Object> values, String key) {
        String value = text(values, key);
        if (value == null) {
            return null;
        }
        try {
            return Date.valueOf(value);                     // already YYYY-MM-DD
        } catch (IllegalArgumentException ignored) {
            // fall through to the locale forms below
        }

        java.util.regex.Matcher m = SLASH_OR_DASH_DATE.matcher(value);
        if (m.matches()) {
            int first = Integer.parseInt(m.group(1));
            int second = Integer.parseInt(m.group(2));
            int year = Integer.parseInt(m.group(3));

            int day;
            int month;
            if (first > 12 && second <= 12) {
                day = first; month = second;               // unambiguously D/M
            } else if (second > 12 && first <= 12) {
                day = second; month = first;               // unambiguously M/D
            } else {
                day = first; month = second;               // ambiguous -> day first (en-IN)
            }
            try {
                return Date.valueOf(java.time.LocalDate.of(year, month, day));
            } catch (java.time.DateTimeException ex) {
                throw new IllegalArgumentException(
                        key + " is not a valid date: \"" + value + "\".");
            }
        }
        throw new IllegalArgumentException(
                key + " must be YYYY-MM-DD or DD/MM/YYYY, but was \"" + value + "\".");
    }

    private Integer integer(Map<String, Object> values, String key) {
        String value = text(values, key);
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be a number.");
        }
    }

    /**
     * Consent arrives as the option's LABEL, whatever the therapist typed into
     * the form — "I agree to the above terms and conditions", "I consent", and
     * so on. Matching a fixed list of four exact strings rejected almost every
     * real wording, and a rejected submission is one the therapist never sees.
     *
     * Negatives are checked FIRST and on their own: "I do not agree" contains
     * "agree", so an affirmative-substring test alone would read a refusal as
     * consent. That is the one error worth being careful about here — recording
     * consent that was never given is worse than rejecting a valid submission.
     */
    private static final List<String> CONSENT_DENIALS = List.of(
            "do not", "don't", "dont", "not agree", "disagree", "decline", "refuse", "no");

    private static final List<String> CONSENT_AFFIRMATIONS = List.of(
            "agree", "consent", "accept", "yes", "true", "confirm", "understood", "ok");

    private boolean bool(Map<String, Object> values, String key) {
        String value = text(values, key);
        if (value == null) {
            return false;
        }
        String normalised = value.toLowerCase(Locale.ROOT).trim();

        // Exact "no" only — a substring test would match "nothing", "notes"...
        if (normalised.equals("no") || normalised.equals("false")) {
            return false;
        }
        if (CONSENT_DENIALS.stream().anyMatch(d -> !d.equals("no") && normalised.contains(d))) {
            return false;
        }
        return CONSENT_AFFIRMATIONS.stream().anyMatch(normalised::contains);
    }

    private String clean(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}
