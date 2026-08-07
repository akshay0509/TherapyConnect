package com.org.therapistService.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Append-only log of every risk review.
 *
 * Deliberately a review log rather than a change log: a review that confirmed
 * the level was unchanged is itself clinically meaningful, and is exactly the
 * evidence that matters if anyone ever asks what was known and when. So a row
 * is written on every save, with previousLevel equal to level when nothing
 * moved.
 *
 * Free text is not copied here. The current concern and safety plan live on
 * ClientRisk; duplicating every revision would multiply the encrypted copies of
 * the most sensitive text in the system for little clinical gain.
 */
@Entity
@Data
@Table(name = "CLIENT_RISK_REVIEW")
public class ClientRiskReview {

    @Id
    private String reviewId;

    @Column(nullable = false)
    private String therapistId;

    @Column(nullable = false)
    private String clientId;

    @Enumerated(EnumType.STRING)
    private RiskLevel previousLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel level;

    @Column(nullable = false)
    private LocalDateTime reviewedAt = LocalDateTime.now();

    private String reviewedBy;

    @PrePersist
    public void generateId() {
        if (reviewId == null) {
            reviewId = "RRV" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
    }

    /** True when this review confirmed the existing level rather than moving it. */
    public boolean isUnchanged() {
        return previousLevel == level;
    }
}
