package com.org.gatewayService.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@RestController
@RequestMapping("/admin")
public class AdminServicesController {

    // Services expected to be registered in Eureka
    private static final List<String> EXPECTED_SERVICES = List.of(
            "user-service",
            "therapist-service",
            "client-service",
            "appointment-service",
            "analytics-service",
            "notification-service"
    );

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;

    public AdminServicesController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        // Short timeouts — a hung service must not stall the whole dashboard
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(3000);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @GetMapping("/services")
    public ResponseEntity<List<Map<String, Object>>> getServicesHealth() {
        List<Map<String, Object>> result = new ArrayList<>();

        // Gateway itself is UP — this request reached it
        result.add(Map.of("name", "gateway-service", "status", "UP", "instances", 1));

        for (String service : EXPECTED_SERVICES) {
            List<ServiceInstance> instances = discoveryClient.getInstances(service);
            if (instances == null || instances.isEmpty()) {
                result.add(Map.of("name", service, "status", "DOWN", "instances", 0,
                        "detail", "Not registered in Eureka"));
                continue;
            }

            String status = pingHealth(instances.get(0));
            result.add(Map.of("name", service, "status", status, "instances", instances.size()));
        }

        return ResponseEntity.ok(result);
    }

    private String pingHealth(ServiceInstance instance) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.get()
                    .uri(instance.getUri() + "/actuator/health")
                    .retrieve()
                    .body(Map.class);
            return body != null && "UP".equals(body.get("status")) ? "UP" : "DEGRADED";
        } catch (RestClientResponseException e) {
            // Service answered with an HTTP error (e.g. 401 — actuator is JWT-protected).
            // Any HTTP response means the process is alive.
            return "UP";
        } catch (Exception e) {
            // Registered in Eureka but not answering at all
            return "UNREACHABLE";
        }
    }
}
