package com.org.appointmentService.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.appointmentService.Entity.TherapyDeliveryMode;

@Repository
public interface TherapyDeliveryModeRepository extends JpaRepository<TherapyDeliveryMode, String> {

    Optional<TherapyDeliveryMode> findByModeIdAndTherapistIdAndServiceIdAndIsActiveTrue(
            String modeId, String therapistId, String serviceId);
}
