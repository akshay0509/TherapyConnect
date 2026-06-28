package com.org.analyticsService.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.org.analyticsService.Dto.AnalyticsSummaryDto;
import com.org.analyticsService.Dto.DailySnapshotDto;
import com.org.analyticsService.Dto.ServiceBreakdownDto;
import com.org.analyticsService.Services.AnalyticsAggregationService;
import com.org.analyticsService.Utility.SecurityUtils;

@RestController
public class AnalyticsController {

    @Autowired
    private AnalyticsAggregationService analyticsAggregationService;

    @GetMapping("/daily")
    public ResponseEntity<List<DailySnapshotDto>> getDailySnapshots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        String therapistId = SecurityUtils.getTherapistId();
        return ResponseEntity.ok(analyticsAggregationService.getDailySnapshots(therapistId, from, to));
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDto> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        String therapistId = SecurityUtils.getTherapistId();
        return ResponseEntity.ok(analyticsAggregationService.getSummary(therapistId, from, to));
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceBreakdownDto>> getServiceBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        String therapistId = SecurityUtils.getTherapistId();
        return ResponseEntity.ok(analyticsAggregationService.getServiceBreakdown(therapistId, from, to));
    }
}
