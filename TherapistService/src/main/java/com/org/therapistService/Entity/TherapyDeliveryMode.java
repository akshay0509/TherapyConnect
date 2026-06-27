package com.org.therapistService.Entity;

import java.math.BigDecimal;
import java.util.UUID;

import com.org.therapistService.Enums.DeliveryModeType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPY_DELIVERY_MODES", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"therapistId", "serviceId", "displayName"})
})
public class TherapyDeliveryMode {

    @Id
    private String modeId;

    @Column(nullable = false)
    private String therapistId;

    @Column(nullable = false)
    private String serviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryModeType modeType;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = true)
    private String address;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean isActive = true;

    @PrePersist
    public void generateId() {
        if (this.modeId == null) {
            String uniquePart = UUID.randomUUID().toString().substring(0, 8);
            this.modeId = "MOD" + uniquePart;
        }
    }
}
