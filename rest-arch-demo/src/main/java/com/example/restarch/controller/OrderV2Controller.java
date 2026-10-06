package com.example.restarch.controller;

import com.example.restarch.dto.ErrorResponse;
import com.example.restarch.dto.OrderDetailResponse;
import com.example.restarch.dto.OrderUpdateRequest;
import com.example.restarch.dto.PageResponse;
import com.example.restarch.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v2 of the order read/update API. Differences from v1: the collection endpoint is
 * paginated and sortable (?page=0&size=20&sort=createdAt,desc) and returns a PageResponse
 * envelope instead of a bare array. Order creation stays on v1.
 */
@RestController
@RequestMapping("/api/v2/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Orders v2", description = "Paginated order queries and updates")
public class OrderV2Controller {

    private final OrderService orderService;

    @Operation(summary = "List orders (paginated)",
            description = "Query params: page (0-based), size (max 100), sort=field,asc|desc. "
                    + "Sortable fields: any Order property, e.g. createdAt, status, price.")
    @GetMapping
    public PageResponse<OrderDetailResponse> getOrders(
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Authentication authentication) {
        return PageResponse.of(orderService.getOrders(authentication.getName(), pageable),
                OrderDetailResponse::from);
    }

    @Operation(summary = "Get an order by id")
    @ApiResponse(responseCode = "404", description = "No such order for this client",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrder(@PathVariable String orderId,
                                        @Parameter(hidden = true) Authentication authentication) {
        return OrderDetailResponse.from(orderService.getOrder(authentication.getName(), orderId));
    }

    @Operation(summary = "Update an order",
            description = "Amends quantity and price. Only allowed while the order is still CREATED.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No such order for this client",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Order already dispatched",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{orderId}")
    public OrderDetailResponse updateOrder(@PathVariable String orderId,
                                           @Valid @RequestBody OrderUpdateRequest request,
                                           @Parameter(hidden = true) Authentication authentication) {
        return OrderDetailResponse.from(orderService.updateOrder(authentication.getName(), orderId, request));
    }
}
