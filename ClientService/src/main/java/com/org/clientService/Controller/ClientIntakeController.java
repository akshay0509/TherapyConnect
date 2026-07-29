package com.org.clientService.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.clientService.Dto.ApproveIntakeRequest;
import com.org.clientService.Dto.ClientIntakeDto;
import com.org.clientService.Dto.IntakeDecisionResponse;
import com.org.clientService.Dto.RejectIntakeRequest;
import com.org.clientService.Entity.ClientIntake.Status;
import com.org.clientService.Services.ClientIntakeService;
import com.org.clientService.Utility.SecurityUtils;

@RestController
@RequestMapping("/intakes")
public class ClientIntakeController {

    private final ClientIntakeService intakeService;

    public ClientIntakeController(ClientIntakeService intakeService) {
        this.intakeService = intakeService;
    }

    @GetMapping
    public List<ClientIntakeDto> list(@RequestParam(defaultValue = "PENDING") Status status) {
        return intakeService.list(SecurityUtils.getTherapistId(), status);
    }

    @GetMapping("/{intakeId}")
    public ClientIntakeDto get(@PathVariable String intakeId) {
        return intakeService.get(SecurityUtils.getTherapistId(), intakeId);
    }

    @PostMapping("/{intakeId}/approve")
    public ResponseEntity<IntakeDecisionResponse> approve(
            @PathVariable String intakeId, @RequestBody ApproveIntakeRequest request)
            throws JsonProcessingException {
        return ResponseEntity.ok(intakeService.approve(
                SecurityUtils.getTherapistId(), SecurityUtils.getUserId(), intakeId, request));
    }

    @PostMapping("/{intakeId}/reject")
    public ResponseEntity<IntakeDecisionResponse> reject(
            @PathVariable String intakeId, @RequestBody(required = false) RejectIntakeRequest request) {
        return ResponseEntity.ok(intakeService.reject(
                SecurityUtils.getTherapistId(), SecurityUtils.getUserId(), intakeId, request));
    }
}
