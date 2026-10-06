package com.example.restarch.controller;

import com.example.restarch.config.OpenApiConfig;
import com.example.restarch.config.ServiceBProperties;
import com.example.restarch.dto.ServiceBCallbackRequest;
import com.example.restarch.entity.OrderStatus;
import com.example.restarch.service.OrderService;
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
 * Service-to-service inbound endpoint - deliberately outside the client JWT
 * flow (Service B is a different trust domain) and authenticated with a
 * shared secret instead. Excluded from JwtAuthFilter/RateLimitFilter and
 * permitAll'd in SecurityConfig; see those for the path exclusion.
 *
 * Demo-only auth: a static shared secret. Use mTLS or a proper
 * service-to-service token exchange in production.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Service B callback", description = "Inbound result delivery from Service B")
public class OrderCallbackController {

    private final OrderService orderService;
    private final ServiceBProperties properties;

    @Operation(
            summary = "Receive an order result from Service B",
            description = """
                    Called by Service B once it finishes processing. The order is matched by
                    correlation ID and moved to COMPLETED or FAILED. Authenticated with the
                    shared X-Service-Key secret, not a client JWT.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Result applied"),
            @ApiResponse(responseCode = "400", description = "Validation failure"),
            @ApiResponse(responseCode = "403", description = "Invalid service key")
    })
    @SecurityRequirement(name = OpenApiConfig.SERVICE_KEY_SCHEME)
    @PostMapping("/callback")
    public ResponseEntity<Void> serviceBCallback(
            @Parameter(hidden = true) @RequestHeader("X-Service-Key") String serviceKey,
            @Valid @RequestBody ServiceBCallbackRequest request) {

        if (!properties.getInboundServiceKey().equals(serviceKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        OrderStatus resultStatus = "COMPLETED".equalsIgnoreCase(request.getStatus())
                ? OrderStatus.COMPLETED
                : OrderStatus.FAILED;

        orderService.applyServiceBCallback(request.getCorrelationId(), resultStatus, request.getMessage());
        return ResponseEntity.ok().build();
    }
}
