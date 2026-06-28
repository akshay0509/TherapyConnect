package com.org.analyticsService.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@IdClass(ClientDsfProjectionId.class)
@Table(name = "ANALYTICS_CLIENT_DSF")
public class ClientDsfProjection {

    @Id
    private String clientId;

    @Id
    private String therapistId;

    private boolean dsf;
}
