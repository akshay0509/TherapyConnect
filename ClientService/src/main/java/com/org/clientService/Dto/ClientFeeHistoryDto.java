package com.org.clientService.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ClientFeeHistoryDto {

	private String feeHistoryId;
	private BigDecimal oldFee;
	private BigDecimal newFee;
	private LocalDateTime changedAt;
}
