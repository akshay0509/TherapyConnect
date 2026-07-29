package com.org.therapistService.Dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * Per-client aggregates over completed appointments, produced by one grouped
 * query so the clients list never fans out into a request per client.
 *
 * A client with no completed sessions simply has no row here — the caller reads
 * that as zero rather than as missing data.
 */
@Data
public class ClientEnrichmentDto {

	private String clientId;
	private long sessionCount;
	private LocalDateTime lastSeen;
	private long pendingNotes;
	/** DSF clients' pending notes never feed the practice-wide total (owner, 29 Jul). */
	private boolean dsf;

	/**
	 * The numeric parameters are declared as {@link Number} deliberately. A JPQL
	 * constructor expression is resolved by exact parameter type, and whether
	 * Hibernate returns Long or Integer for COUNT and for SUM(CASE ...) is not
	 * something to bet a runtime failure on.
	 */
	public ClientEnrichmentDto(
			String clientId,
			Number sessionCount,
			LocalDateTime lastSeen,
			Number pendingNotes,
			Boolean dsf) {
		this.clientId = clientId;
		this.sessionCount = sessionCount == null ? 0L : sessionCount.longValue();
		this.lastSeen = lastSeen;
		this.pendingNotes = pendingNotes == null ? 0L : pendingNotes.longValue();
		this.dsf = dsf != null && dsf;
	}
}
