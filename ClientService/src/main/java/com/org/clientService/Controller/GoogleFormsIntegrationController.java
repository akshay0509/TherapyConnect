package com.org.clientService.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.clientService.Dto.ClientIntakeDto;
import com.org.clientService.Dto.GoogleFormSubmission;
import com.org.clientService.Services.ClientIntakeService;
import com.org.clientService.Services.GoogleFormsSignatureVerifier;

@RestController
@RequestMapping("/google-forms")
public class GoogleFormsIntegrationController {

    private final GoogleFormsSignatureVerifier signatureVerifier;
    private final ClientIntakeService intakeService;
    private final ObjectMapper objectMapper;

    public GoogleFormsIntegrationController(
            GoogleFormsSignatureVerifier signatureVerifier,
            ClientIntakeService intakeService,
            ObjectMapper objectMapper) {
        this.signatureVerifier = signatureVerifier;
        this.intakeService = intakeService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{integrationId}/submissions")
    public ResponseEntity<ClientIntakeDto> receive(
            @PathVariable String integrationId,
            @RequestHeader("X-Integration-Timestamp") String timestamp,
            @RequestHeader("X-Integration-Signature") String signature,
            @RequestBody String rawBody) throws Exception {
        signatureVerifier.verify(integrationId, timestamp, signature, rawBody);
        GoogleFormSubmission submission = objectMapper.readValue(rawBody, GoogleFormSubmission.class);
        ClientIntakeDto staged = intakeService.stage(integrationId, submission);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(staged);
    }
}
