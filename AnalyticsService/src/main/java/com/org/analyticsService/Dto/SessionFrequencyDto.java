package com.org.analyticsService.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SessionFrequencyDto {
    private String bucket;
    private int clientCount;
}
