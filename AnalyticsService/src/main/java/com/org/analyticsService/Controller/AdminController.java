package com.org.analyticsService.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.org.analyticsService.Dto.AnalyticsAdminHealthDto;
import com.org.analyticsService.Repository.AnalyticsDailyRepository;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AnalyticsDailyRepository analyticsDailyRepository;

    @GetMapping("/health")
    public ResponseEntity<AnalyticsAdminHealthDto> health() {
        AnalyticsAdminHealthDto dto = new AnalyticsAdminHealthDto();
        dto.setServerTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Optional<LocalDate> maxDate = analyticsDailyRepository.findMaxDate();

        if (maxDate.isEmpty()) {
            dto.setStatus("NO_DATA");
            dto.setLastProcessedDate(null);
            dto.setDaysBehind(0);
        } else {
            LocalDate last = maxDate.get();
            long behind = ChronoUnit.DAYS.between(last, LocalDate.now());
            dto.setLastProcessedDate(last.format(DateTimeFormatter.ISO_LOCAL_DATE));
            dto.setDaysBehind(behind);
            dto.setStatus(behind > 0 ? "BEHIND" : "OK");
        }

        return ResponseEntity.ok(dto);
    }
}
