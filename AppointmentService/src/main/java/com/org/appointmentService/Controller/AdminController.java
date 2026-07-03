package com.org.appointmentService.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.org.appointmentService.Dto.AdminHealthDto;
import com.org.appointmentService.Services.AdminService;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/health")
    public ResponseEntity<AdminHealthDto> health() {
        return ResponseEntity.ok(adminService.getHealth());
    }

    @PostMapping("/outbox/replay")
    public ResponseEntity<Map<String, Object>> replayOutbox(@RequestBody(required = false) Map<String, String> body) {
        String fromStr = body != null ? body.get("from") : null;
        LocalDateTime from;
        if (fromStr != null && !fromStr.isBlank()) {
            try {
                from = LocalDateTime.parse(fromStr);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid datetime format. Use ISO format: YYYY-MM-DDTHH:MM:SS"));
            }
        } else {
            from = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        }

        Map<String, Object> result = adminService.replayOutbox(from);
        return ResponseEntity.ok(result);
    }
}
