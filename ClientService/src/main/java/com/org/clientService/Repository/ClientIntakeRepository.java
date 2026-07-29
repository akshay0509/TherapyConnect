package com.org.clientService.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.clientService.Entity.ClientIntake;
import com.org.clientService.Entity.ClientIntake.Status;

public interface ClientIntakeRepository extends JpaRepository<ClientIntake, String> {
    Optional<ClientIntake> findByIntegrationIdAndResponseId(String integrationId, String responseId);
    Optional<ClientIntake> findByTherapistIdAndIntakeId(String therapistId, String intakeId);
    List<ClientIntake> findByTherapistIdAndStatusOrderBySubmittedAtDesc(String therapistId, Status status);
}
