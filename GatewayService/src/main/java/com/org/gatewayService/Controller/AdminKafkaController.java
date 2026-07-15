package com.org.gatewayService.Controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.org.gatewayService.Services.KafkaMonitorService;

/**
 * Kafka/DLQ monitoring for the admin dashboard. Protected by the gateway's
 * /admin/** ADMIN-authority rule in SecurityConfig.
 */
@RestController
@RequestMapping("/admin/kafka")
public class AdminKafkaController {

    private static final Logger logger = LoggerFactory.getLogger(AdminKafkaController.class);

    @Autowired
    private KafkaMonitorService kafkaMonitorService;

    @GetMapping
    public ResponseEntity<?> overview() {
        try {
            return ResponseEntity.ok(kafkaMonitorService.overview());
        } catch (Exception e) {
            logger.error("Kafka overview failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Kafka unreachable: " + e.getMessage()));
        }
    }

    @GetMapping("/dlt/{topic}/messages")
    public ResponseEntity<?> peek(@PathVariable String topic,
                                  @RequestParam(defaultValue = "20") int limit) {
        try {
            List<Map<String, Object>> messages = kafkaMonitorService.peekDlt(topic, limit);
            return ResponseEntity.ok(messages);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("DLT peek failed for topic={}", topic, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/dlt/{topic}/replay")
    public ResponseEntity<?> replay(@PathVariable String topic) {
        try {
            return ResponseEntity.ok(kafkaMonitorService.replayDlt(topic));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("DLT replay failed for topic={}", topic, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
