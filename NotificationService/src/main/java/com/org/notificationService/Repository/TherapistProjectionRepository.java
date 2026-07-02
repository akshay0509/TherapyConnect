package com.org.notificationService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.notificationService.Entity.TherapistProjection;

public interface TherapistProjectionRepository extends JpaRepository<TherapistProjection, String> {}
