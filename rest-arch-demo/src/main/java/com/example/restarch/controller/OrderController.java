package com.example.restarch.controller;

import com.example.restarch.dto.ErrorResponse;
import com.example.restarch.dto.OrderDetailResponse;
import com.example.restarch.dto.OrderRequest;
import com.example.restarch.dto.OrderResponse;
import com.example.restarch.dto.OrderUpdateRequest;
import com.example.restarch.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Orders", description = "Client-facing order submission")
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "Submit an order",
            description = """
                    Accepts the order, queues it for async persistence and dispatches it to Service B.
                    The call is idempotent on Idempotency-Key: replaying a key returns the original
                    result with 200 and replay=true instead of creating a second order.""")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Order accepted for processing"),
            @ApiResponse(responseCode = "200", description = "Replay of a previously seen Idempotency-Key"),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing, malformed or expired bearer token",
                    content = @Content),
            @ApiResponse(responseCode = "429", description = "Per-client rate limit exceeded", content = @Content),
            @ApiResponse(responseCode = "503", description = "Persistence queue saturated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Parameter(description = "Client-generated key that makes retries safe", required = true,
                    example = "3f1c9a52-2b0e-4a1f-9f6d-0a1b2c3d4e5f")
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody OrderRequest request,
            @Parameter(hidden = true) Authentication authentication) {

        OrderResponse response = orderService.processOrder(authentication.getName(), idempotencyKey, request);

        HttpStatus status = response.isReplay() ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(response);
    }

    @Operation(summary = "Get an order by id", description = "Returns the caller's own order only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "No such order for this client",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrder(@PathVariable String orderId,
                                        @Parameter(hidden = true) Authentication authentication) {
        return OrderDetailResponse.from(orderService.getOrder(authentication.getName(), orderId));
    }

    @Operation(summary = "List all orders (unpaged)",
            description = "v1 returns every order of the caller, newest first. Prefer GET /api/v2/orders for pagination.")
    @GetMapping
    public List<OrderDetailResponse> getAllOrders(@Parameter(hidden = true) Authentication authentication) {
        return orderService.getAllOrders(authentication.getName()).stream()
                .map(OrderDetailResponse::from)
                .toList();
    }

    @Operation(summary = "Update an order",
            description = "Amends quantity and price. Only allowed while the order is still CREATED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order updated"),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No such order for this client",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Order already dispatched; no longer modifiable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{orderId}")
    public OrderDetailResponse updateOrder(@PathVariable String orderId,
                                           @Valid @RequestBody OrderUpdateRequest request,
                                           @Parameter(hidden = true) Authentication authentication) {
        return OrderDetailResponse.from(orderService.updateOrder(authentication.getName(), orderId, request));
    }
}
