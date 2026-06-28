package com.org.analyticsService.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.analyticsService.Entity.AnalyticsServiceDaily;
import com.org.analyticsService.Entity.AnalyticsServiceDailyId;

@Repository
public interface AnalyticsServiceDailyRepository extends JpaRepository<AnalyticsServiceDaily, AnalyticsServiceDailyId> {

    List<AnalyticsServiceDaily> findByTherapistIdAndDateBetween(
            String therapistId, LocalDate from, LocalDate to);
}
