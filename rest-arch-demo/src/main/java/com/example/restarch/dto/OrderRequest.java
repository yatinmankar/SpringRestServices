package com.example.restarch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "An order to submit for processing")
public class OrderRequest {

    @NotBlank
    @Schema(description = "Instrument being traded", example = "AAPL")
    private String instrumentId;

    @Positive
    @Schema(description = "Number of units", example = "100")
    private int quantity;

    @Positive
    @Schema(description = "Limit price per unit", example = "187.45")
    private double price;

    @NotNull
    @Schema(description = "Order direction", example = "BUY")
    private Side side;

    public enum Side { BUY, SELL }
}
