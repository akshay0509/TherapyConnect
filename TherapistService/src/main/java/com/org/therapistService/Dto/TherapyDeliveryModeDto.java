package com.org.therapistService.Dto;

import java.math.BigDecimal;

import com.org.therapistService.Enums.DeliveryModeType;

import lombok.Data;

@Data
public class TherapyDeliveryModeDto {

    private String modeId;
    private String therapistId;
    private String serviceId;
    private DeliveryModeType modeType;
    private String displayName;
    private String address;
    private BigDecimal price;
    private Boolean isActive;
}
