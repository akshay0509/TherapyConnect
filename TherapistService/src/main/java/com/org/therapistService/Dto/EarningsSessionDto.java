package com.org.therapistService.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.org.events.TherapistAppointment.AppointmentStatus;

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
    /**
     * The transactions list now contains no-shows as well as delivered sessions
     * (both bill), so the row has to say which it is — otherwise a therapist
     * cannot tell why they were paid for a session nobody attended.
     */
    private AppointmentStatus status;

    public EarningsSessionDto(
            String appointmentId,
            String clientId,
            String clientName,
            String serviceId,
            String modeId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal sessionFee,
            boolean dsf,
            AppointmentStatus status) {
        this.appointmentId = appointmentId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.serviceId = serviceId;
        this.modeId = modeId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sessionFee = sessionFee == null ? BigDecimal.ZERO : sessionFee;
        this.dsf = dsf;
        /*
         * The stamped fee IS the answer. resolveSessionFee writes sessionFee = 0
         * at booking for a pro-bono client, so a zero fee already means zero
         * earned and no second test is needed.
         *
         * Reading the dsf flag here read the client's status TODAY and applied it
         * to sessions billed months ago: a client who paid normally and later
         * moved to DSF had every past session's Earning column rewritten to zero
         * while the Fee column still showed what they actually paid. The two
         * columns contradicted each other and the totals under-reported real
         * income. Same retroactivity the earnings queries were fixed for.
         */
        this.earningAmount = this.sessionFee;
        this.status = status;
    }
}
