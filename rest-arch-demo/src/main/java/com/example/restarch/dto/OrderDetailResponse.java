package com.example.restarch.dto;

import com.example.restarch.entity.Order;
import com.example.restarch.entity.OrderStatus;
import com.example.restarch.entity.RiskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Full view of a persisted order")
public record OrderDetailResponse(
        String orderId,
        String correlationId,
        String instrumentId,
        int quantity,
        BigDecimal price,
        String side,
        OrderStatus status,
        RiskStatus riskStatus,
        Integer riskScore,
        String riskMessage,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderDetailResponse from(Order o) {
        return new OrderDetailResponse(o.getId(), o.getCorrelationId(), o.getInstrumentId(),
                o.getQuantity(), o.getPrice(), o.getSide(), o.getStatus(),
                o.getRiskStatus(), o.getRiskScore(), o.getRiskMessage(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
