package com.org.therapistService.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class EarningsSessionDto {

    private String appointmentId;
    private String clientId;
    private String clientName;
    private String serviceId;
    private String modeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal sessionFee;
    private boolean dsf;
    private BigDecimal earningAmount;

    public EarningsSessionDto(
            String appointmentId,
            String clientId,
            String clientName,
            String serviceId,
            String modeId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal sessionFee,
            boolean dsf) {
        this.appointmentId = appointmentId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.serviceId = serviceId;
        this.modeId = modeId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sessionFee = sessionFee == null ? BigDecimal.ZERO : sessionFee;
        this.dsf = dsf;
        this.earningAmount = dsf ? BigDecimal.ZERO : this.sessionFee;
    }
}
