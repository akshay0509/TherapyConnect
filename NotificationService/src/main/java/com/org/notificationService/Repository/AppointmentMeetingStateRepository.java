package com.org.notificationService.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.org.notificationService.Entity.AppointmentMeetingState;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;

public interface AppointmentMeetingStateRepository extends JpaRepository<AppointmentMeetingState, String> {
    @Modifying
    @Query(value = "INSERT INTO appointment_meeting_state (appointment_id, calendar_revision, meeting_sequence, retry_count) VALUES (:id, 0, 0, 0) ON CONFLICT (appointment_id) DO NOTHING", nativeQuery = true)
    void ensureRow(String id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AppointmentMeetingState s WHERE s.appointmentId = :id")
    Optional<AppointmentMeetingState> lockState(String id);
    List<AppointmentMeetingState> findTop25ByMeetingStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(String status, LocalDateTime now);
}
