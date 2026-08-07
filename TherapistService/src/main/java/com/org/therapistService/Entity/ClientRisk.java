package com.org.therapistService.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.org.therapistService.Utility.SessionNotesEncryptor;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

/**
 * The current risk assessment for one client, as recorded by their therapist.
 *
 * One row per (therapistId, clientId) — this is the CURRENT position. Every
 * review that produced it, including reviews that changed nothing, is kept in
 * ClientRiskReview.
 *
 * Lives in TherapistService rather than ClientService because it is clinical
 * rather than demographic, and because the free-text fields reuse the same
 * encryption already protecting session notes.
 */
@Entity
@Data
@Table(
    name = "CLIENT_RISK",
    uniqueConstraints = @UniqueConstraint(columnNames = { "therapistId", "clientId" })
)
public class ClientRisk {

    @Id
    private String riskId;

    @Column(nullable = false)
    private String therapistId;

    @Column(nullable = false)
    private String clientId;

    /* STRING, never the JPA default ORDINAL. An ordinal column stores position,
       so inserting or reordering a level would silently re-label every existing
       row — and on a clinical risk record that is the worst possible field for
       it to happen to. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel level = RiskLevel.NONE;

    /** What the risk actually is, in the therapist's words. Encrypted at rest. */
    @Convert(converter = SessionNotesEncryptor.class)
    @Column(columnDefinition = "TEXT")
    private String concern;

    /** What has been agreed with the client. The field that matters in a
     *  crisis, and the one most systems omit. Encrypted at rest. */
    @Convert(converter = SessionNotesEncryptor.class)
    @Column(columnDefinition = "TEXT")
    private String safetyPlan;

    /** An assessment from eight months ago is not a current assessment. This is
     *  what the review prompt is derived from. */
    private LocalDateTime lastReviewedAt;

    private String lastReviewedBy;

    /**
     * Per-client override of the cadence implied by the level, for a client
     * whose circumstances warrant a different rhythm. Null means use the
     * level's own interval.
     */
    private Integer reviewIntervalDaysOverride;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    @PrePersist
    public void generateId() {
        if (riskId == null) {
            riskId = "RSK" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
    }

    /** Null when no review is ever due — either the level is NONE or the client
     *  has never been assessed. */
    public Integer effectiveIntervalDays() {
        if (reviewIntervalDaysOverride != null) {
            return reviewIntervalDaysOverride;
        }
        return level == null ? null : level.getReviewIntervalDays();
    }

    public LocalDateTime reviewDueAt() {
        Integer days = effectiveIntervalDays();
        if (days == null || lastReviewedAt == null) {
            return null;
        }
        return lastReviewedAt.plusDays(days);
    }
}
