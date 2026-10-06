package com.example.restarch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Risk verdict delivered by the risk service")
public class RiskResultRequest {

    @NotBlank
    @Schema(description = "Correlation ID of the original order", example = "c0ffee-1234")
    private String correlationId;

    @Schema(description = "Order identifier, informational", example = "ORD-8f3c1a")
    private String orderId;

    @NotBlank
    @Pattern(regexp = "APPROVED|REJECTED")
    @Schema(description = "Verdict", allowableValues = {"APPROVED", "REJECTED"}, example = "APPROVED")
    private String decision;

    @Schema(description = "Risk score 0-100, higher is riskier", example = "12")
    private Integer score;

    @Schema(description = "Optional detail from the risk service", example = "notional within limit")
    private String message;
}
