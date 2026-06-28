package com.org.analyticsService.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.analyticsService.Entity.ClientDsfProjection;
import com.org.analyticsService.Entity.ClientDsfProjectionId;

@Repository
public interface ClientDsfProjectionRepository extends JpaRepository<ClientDsfProjection, ClientDsfProjectionId> {

    Optional<ClientDsfProjection> findByClientIdAndTherapistId(String clientId, String therapistId);
}
