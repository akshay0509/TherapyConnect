package com.org.clientService.Entity;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.Type;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Data;

@Entity
@Data
@Table(name = "CLIENT_INTAKE", uniqueConstraints = {
        @UniqueConstraint(name = "uk_intake_integration_response", columnNames = {"integrationId", "responseId"})
})
public class ClientIntake {

    public enum Status { PENDING, APPROVED, REJECTED }
    @Id
    private String intakeId;
    @Column(nullable = false)
    private String integrationId;
    @Column(nullable = false)
    private String responseId;
    @Column(nullable = false)
    private String therapistId;
    private LocalDateTime submittedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;
    private String clientId;
    @Column(length = 500)
    private String rejectionReason;

    private String fullName;
    private String firstName;
    private String lastName;
    private Date dob;
    private String pronouns;
    private String gender;
    private String qualification;
    private String occupation;
    private String phoneNumber;
    private String email;
    private String preferredDays;
    private String preferredTimings;
    private String preferredModes;
    private String emergencyContactName;
    private Integer emergencyContactAge;
    private String emergencyContactRelationship;
    private String emergencyPhoneNumber;
    private boolean consent;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> rawAnswers = new LinkedHashMap<>();

    @Version
    private long version;

    @PrePersist
    public void beforeInsert() {
        if (intakeId == null) {
            intakeId = "INT" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}
