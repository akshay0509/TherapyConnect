package com.org.therapistService.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.therapistService.Entity.ClientRiskReview;

public interface ClientRiskReviewRepository extends JpaRepository<ClientRiskReview, String> {

	List<ClientRiskReview> findByTherapistIdAndClientIdOrderByReviewedAtDesc(String therapistId, String clientId);
}
