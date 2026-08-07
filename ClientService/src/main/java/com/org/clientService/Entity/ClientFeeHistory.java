package com.org.clientService.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Append-only record of every change to a client's negotiated rate.
 *
 * Money needs an answer to "why was this client charged that?". The session fee
 * is stamped onto the appointment at booking, so past appointments are already
 * safe from a later rate change — but without this table there is no way to see
 * when the rate moved, or what it was before.
 *
 * Written in the same transaction as the client update rather than derived from
 * the ClientUpdated event: a consumer could not tell a real rate change from a
 * replay, and the history would drift from the record it describes.
 */
@Entity
@Data
@Table(name = "CLIENT_FEE_HISTORY")
public class ClientFeeHistory {

	@Id
	private String feeHistoryId;

	private String clientId;
	private String therapistId;

	/** Null means "no negotiated rate" — on either side of the change. */
	private BigDecimal oldFee;
	private BigDecimal newFee;

	private LocalDateTime changedAt = LocalDateTime.now();

	@PrePersist
	public void generateId() {
		if (feeHistoryId == null) {
			feeHistoryId = "FEE" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		}
	}
}
