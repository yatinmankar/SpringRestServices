package com.example.restarch.dto;

import java.math.BigDecimal;

/** Outbound to the risk service's POST /api/v1/risk/check. */
public record RiskCheckRequest(
        String correlationId,
        String orderId,
        String clientId,
        String instrumentId,
        int quantity,
        BigDecimal price,
        String side) {
}
