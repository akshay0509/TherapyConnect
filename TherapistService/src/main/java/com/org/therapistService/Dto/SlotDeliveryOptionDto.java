package com.org.therapistService.Dto;

import java.math.BigDecimal;

import com.org.therapistService.Enums.DeliveryModeType;

import lombok.Data;

@Data
public class SlotDeliveryOptionDto {

    private String modeId;
    private String displayName;
    private DeliveryModeType modeType;
    private String address;
    private BigDecimal price;
}
