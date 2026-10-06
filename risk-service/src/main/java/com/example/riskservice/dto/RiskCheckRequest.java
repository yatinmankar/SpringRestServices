package com.example.riskservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** Inbound from the order service. */
@Data
public class RiskCheckRequest {

    @NotBlank
    private String correlationId;

    @NotBlank
    private String orderId;

    @NotBlank
    private String clientId;

    @NotBlank
    private String instrumentId;

    @Positive
    private int quantity;

    @Positive
    private double price;

    @NotBlank
    private String side;
}
