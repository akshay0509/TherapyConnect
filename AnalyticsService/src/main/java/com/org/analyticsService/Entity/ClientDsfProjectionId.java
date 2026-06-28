package com.org.analyticsService.Entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDsfProjectionId implements Serializable {
    private String clientId;
    private String therapistId;
}
