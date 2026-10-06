package com.example.restarch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Mutable fields of an order that has not yet been sent to Service B")
public class OrderUpdateRequest {

    @Positive
    @Schema(description = "New number of units", example = "150")
    private int quantity;

    @Positive
    @Schema(description = "New limit price per unit", example = "190.10")
    private double price;
}
