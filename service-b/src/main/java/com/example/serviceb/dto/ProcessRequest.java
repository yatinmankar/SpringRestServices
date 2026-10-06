package com.example.serviceb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Unit of work dispatched by Service 1")
public class ProcessRequest {

    @NotBlank
    @Schema(description = "Correlation ID to thread through processing and the callback",
            example = "c0ffee-1234")
    private String correlationId;

    @NotBlank
    @Schema(description = "Order identifier assigned by Service 1", example = "ORD-8f3c1a")
    private String orderId;

    @NotBlank
    @Schema(description = "Instrument being traded", example = "AAPL")
    private String instrumentId;

    @Positive
    @Schema(description = "Number of units", example = "100")
    private int quantity;

    @Positive
    @Schema(description = "Limit price per unit", example = "187.45")
    private double price;

    @NotBlank
    @Schema(description = "Order direction", allowableValues = {"BUY", "SELL"}, example = "BUY")
    private String side;
}
