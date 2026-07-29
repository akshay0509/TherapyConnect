package com.org.clientService.Dto;

import com.org.clientService.Entity.ClientIntake.Status;

public record IntakeDecisionResponse(String intakeId, Status status, String clientId) {}
