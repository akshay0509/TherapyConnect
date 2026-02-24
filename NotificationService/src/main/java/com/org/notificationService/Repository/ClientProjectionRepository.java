package com.org.notificationService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.notificationService.Entity.ClientProjection;

@Repository
public interface ClientProjectionRepository extends JpaRepository<ClientProjection, String> {

}
