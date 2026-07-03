package com.org.appointmentService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.appointmentService.Entity.TherapistPaymentProjection;

public interface TherapistPaymentProjectionRepository extends JpaRepository<TherapistPaymentProjection, String> {
}
