package com.org.appointmentService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.appointmentService.Entity.ClientContactProjection;

public interface ClientContactProjectionRepository extends JpaRepository<ClientContactProjection, String> {
}
