package com.example.riskservice.controller;

import com.example.riskservice.correlation.CorrelationContext;
import com.example.riskservice.dto.RiskCheckRequest;
import com.example.riskservice.service.RiskEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
public class RiskCheckController {

    private final RiskEvaluationService evaluationService;

    /**
     * Returns 202 immediately; the verdict is delivered by POSTing to the order
     * service's risk-result endpoint, so nothing is returned in this body.
     */
    @PostMapping("/check")
    public ResponseEntity<Void> check(@Valid @RequestBody RiskCheckRequest request) {
        evaluationService.evaluateAsync(request, CorrelationContext.get());
        return ResponseEntity.accepted().build();
    }
}
