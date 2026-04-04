package com.org.appointmentService.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.appointmentService.Entity.TherapistAvailabilityOverride;

public interface TherapistAvailabilityOverrideRepository extends JpaRepository<TherapistAvailabilityOverride, String> { // CODEX-OVERRIDE-PROJECTION-HYBRID

    List<TherapistAvailabilityOverride> findByTherapistIdAndAvailableFalseAndStartTimeLessThanAndEndTimeGreaterThan( // CODEX-OVERRIDE-PROJECTION-HYBRID
            String therapistId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    List<TherapistAvailabilityOverride> findByTherapistIdAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc( // CODEX-OVERRIDE-PROJECTION-HYBRID
            String therapistId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );
}


