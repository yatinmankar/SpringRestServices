package com.example.serviceb.controller;

import com.example.serviceb.correlation.CorrelationContext;
import com.example.serviceb.dto.ProcessRequest;
import com.example.serviceb.service.OrderProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/process")
@RequiredArgsConstructor
@Tag(name = "Processing", description = "Work accepted from Service 1")
public class ProcessController {

    private final OrderProcessingService processingService;

    @Operation(
            summary = "Accept an order for processing",
            description = """
                    Returns 202 immediately and processes off the request thread. The result is
                    delivered by calling Service 1's callback with the same correlation ID, so
                    nothing is returned in this response body.""")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Work accepted; result follows via callback"),
            @ApiResponse(responseCode = "400", description = "Validation failure"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid X-Service-Key")
    })
    @PostMapping
    public ResponseEntity<Void> process(@Valid @RequestBody ProcessRequest request) {

        processingService.processAsync(request, CorrelationContext.get());
        return ResponseEntity.accepted().build();
    }
}
