package com.org.appointmentService.Entity;

import java.math.BigDecimal;

import com.org.appointmentService.Enums.DeliveryModeType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPY_DELIVERY_MODES")
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
}
