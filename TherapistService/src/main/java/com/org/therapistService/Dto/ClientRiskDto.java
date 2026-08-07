package com.org.therapistService.Dto;

import java.time.LocalDateTime;
import java.util.List;

import com.org.therapistService.Entity.RiskLevel;

import lombok.Data;

@Data
public class ClientRiskDto {

	private RiskLevel level;
	private String concern;
	private String safetyPlan;
	private LocalDateTime lastReviewedAt;
	private String lastReviewedBy;
	private Integer reviewIntervalDaysOverride;

	/* Derived server-side so the review rule lives in one place. The UI renders
	   these; it does not recompute them. */
	private Integer effectiveIntervalDays;
	private LocalDateTime reviewDueAt;
	private boolean reviewDue;

	/** True when the client has no assessment yet — distinct from an assessed
	 *  level of NONE, which is a clinical judgement someone actually made. */
	private boolean neverAssessed;

	private List<ClientRiskReviewDto> reviews;
}
