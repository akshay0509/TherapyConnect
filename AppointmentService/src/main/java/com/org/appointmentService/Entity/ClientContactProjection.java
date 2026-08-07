package com.org.appointmentService.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "CLIENT_CONTACT_PROJECTION")
public class ClientContactProjection {

	@Id
	private String clientId;

	private String firstName;
	private String lastName;
	private String email;
	private String phoneNumber;

	/** Negotiated per-client rate; null means the mode price applies. */
	private java.math.BigDecimal sessionFee;

	/**
	 * Pro bono. Overrides every rate: the session is stamped at zero.
	 *
	 * Deliberately the boxed Boolean, not the primitive. This column is added to
	 * a table that already has rows, and Hibernate emits "not null" for a
	 * primitive — which Postgres rejects on a non-empty table. ddl-auto=update
	 * logs that failure and carries on, so the column would silently not exist
	 * and every read of this projection would fail, taking booking down with it.
	 * Null means the same as false: not pro bono.
	 */
	private Boolean dsf;
}
