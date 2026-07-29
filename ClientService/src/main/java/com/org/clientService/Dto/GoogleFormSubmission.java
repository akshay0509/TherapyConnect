package com.org.clientService.Dto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record GoogleFormSubmission(
        String responseId,
        LocalDateTime submittedAt,
        Map<String, Object> answers
) {
    public GoogleFormSubmission {
        answers = answers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(answers);
    }
}
