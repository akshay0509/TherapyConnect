package com.org.therapistService.Dto;

import java.time.LocalDateTime;

import com.org.therapistService.Entity.RiskLevel;

import lombok.Data;

@Data
public class ClientRiskReviewDto {

	private String reviewId;
	private RiskLevel previousLevel;
	private RiskLevel level;
	private LocalDateTime reviewedAt;
	private String reviewedBy;
	private boolean unchanged;
}
