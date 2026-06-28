package com.org.analyticsService.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.analyticsService.Entity.ClientEngagementProjection;
import com.org.analyticsService.Entity.ClientEngagementProjectionId;

public interface ClientEngagementRepository extends JpaRepository<ClientEngagementProjection, ClientEngagementProjectionId> {

    List<ClientEngagementProjection> findByTherapistId(String therapistId);

    Optional<ClientEngagementProjection> findByClientIdAndTherapistId(String clientId, String therapistId);
}
