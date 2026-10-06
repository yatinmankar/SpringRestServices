package com.example.restarch.dto;

import java.math.BigDecimal;

public record ServiceBDispatchRequest(
        String correlationId,
        String orderId,
        String instrumentId,
        int quantity,
        BigDecimal price,
        String side) {
}
