package com.org.clientService.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "OUTBOX_EVENT")
public class OutboxEvent {

	@Id
    private String outboxEventId;

    private String aggregateType;
    private String aggregateId;
    private String eventType;

    @Type(JsonBinaryType.class)
    //@JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode payload;

    private LocalDateTime createdAt;
    private boolean published;

    // poison-event guard: a permanently failing event is parked after
    // exhausting retries so it stops blocking the events behind it.
    // columnDefinition defaults let ddl-auto add the columns to existing rows.
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int retryCount = 0;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean parked = false;

    @PrePersist
    public void generateId() {
        if (this.outboxEventId == null) {
            String uniquePart = UUID.randomUUID().toString().substring(0, 8);
            this.outboxEventId = "OUT" + uniquePart;
        }
    }
}
