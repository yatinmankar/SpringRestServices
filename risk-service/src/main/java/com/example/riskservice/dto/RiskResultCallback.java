package com.example.riskservice.dto;

/**
 * Outbound to the order service's POST /api/v1/orders/risk-result.
 * decision is APPROVED or REJECTED; score is 0-100 (higher = riskier).
 */
public record RiskResultCallback(
        String correlationId,
        String orderId,
        String decision,
        int score,
        String message) {
}
