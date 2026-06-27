package com.org.therapistService.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.therapistService.Entity.TherapyDeliveryMode;

@Repository
public interface TherapyDeliveryModeRepository extends JpaRepository<TherapyDeliveryMode, String> {

    List<TherapyDeliveryMode> findByTherapistId(String therapistId);

    List<TherapyDeliveryMode> findByTherapistIdAndIsActiveTrue(String therapistId);

    List<TherapyDeliveryMode> findByTherapistIdAndServiceIdAndIsActiveTrue(String therapistId, String serviceId);

    Optional<TherapyDeliveryMode> findByModeIdAndTherapistId(String modeId, String therapistId);
}
