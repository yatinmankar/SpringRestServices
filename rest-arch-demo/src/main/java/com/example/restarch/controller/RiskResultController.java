package com.example.restarch.controller;

import com.example.restarch.config.OpenApiConfig;
import com.example.restarch.config.RiskServiceProperties;
import com.example.restarch.dto.RiskResultRequest;
import com.example.restarch.entity.RiskStatus;
import com.example.restarch.service.RiskResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service inbound endpoint for the risk service, authenticated with a
 * shared secret like the Service B callback. Excluded from JwtAuthFilter /
 * RateLimitFilter and permitAll'd in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Risk service callback", description = "Inbound risk verdict delivery from the risk service")
public class RiskResultController {

    private final RiskResultService riskResultService;
    private final RiskServiceProperties properties;

    @Operation(
            summary = "Receive a risk verdict from the risk service",
            description = """
                    Called by the risk service once it has evaluated an order. The order is matched
                    by correlation ID. Idempotent - a repeated delivery for an order that already
                    has a verdict is acknowledged and ignored, so the risk service can retry safely.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verdict applied (or already applied)"),
            @ApiResponse(responseCode = "400", description = "Validation failure"),
            @ApiResponse(responseCode = "403", description = "Invalid service key"),
            @ApiResponse(responseCode = "404", description = "No order with this correlation ID")
    })
    @SecurityRequirement(name = OpenApiConfig.SERVICE_KEY_SCHEME)
    @PostMapping("/risk-result")
    public ResponseEntity<Void> riskResult(
            @Parameter(hidden = true) @RequestHeader("X-Service-Key") String serviceKey,
            @Valid @RequestBody RiskResultRequest request) {

        if (!properties.getInboundServiceKey().equals(serviceKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        RiskStatus status = "APPROVED".equals(request.getDecision()) ? RiskStatus.APPROVED : RiskStatus.REJECTED;
        riskResultService.apply(request.getCorrelationId(), status, request.getScore(), request.getMessage());
        return ResponseEntity.ok().build();
    }
}
