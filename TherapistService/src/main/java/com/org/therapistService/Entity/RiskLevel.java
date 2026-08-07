package com.org.therapistService.Entity;

/**
 * Clinical risk level for a client, as judged and recorded by the therapist.
 *
 * Never derived. The app does not compute this from attendance, cancellations,
 * note content or anything else — an algorithmic risk score is a clinical
 * assessment made by software that has never met the client, and a therapist
 * who either trusts it or is contradicted by it is worse off than with nothing.
 * The app holds the assessment and reminds about it; the therapist makes it.
 *
 * The review interval lives on the level so the cadence follows the judgement
 * automatically: raising someone to HIGH brings their next review forward
 * without anyone reconfiguring anything. Owner-chosen values (07 Aug) —
 * 15 days rather than 14 so the prompt lands the day BEFORE a fortnightly
 * session, leaving time to prepare rather than arriving as the client does.
 */
public enum RiskLevel {

    NONE(null),
    LOW(90),
    MODERATE(60),
    HIGH(15);

    /** Null means no review is ever due. */
    private final Integer reviewIntervalDays;

    RiskLevel(Integer reviewIntervalDays) {
        this.reviewIntervalDays = reviewIntervalDays;
    }

    public Integer getReviewIntervalDays() {
        return reviewIntervalDays;
    }
}
