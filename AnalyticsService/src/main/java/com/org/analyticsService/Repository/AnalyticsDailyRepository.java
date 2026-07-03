package com.org.analyticsService.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.org.analyticsService.Entity.AnalyticsDaily;
import com.org.analyticsService.Entity.AnalyticsDailyId;

@Repository
public interface AnalyticsDailyRepository extends JpaRepository<AnalyticsDaily, AnalyticsDailyId> {

    List<AnalyticsDaily> findByTherapistIdAndDateBetweenOrderByDateAsc(
            String therapistId, LocalDate from, LocalDate to);

    @Query("SELECT MAX(a.date) FROM AnalyticsDaily a")
    Optional<LocalDate> findMaxDate();
}
