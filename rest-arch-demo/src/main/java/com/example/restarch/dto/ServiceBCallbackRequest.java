package com.example.restarch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Processing result delivered by Service B")
public class ServiceBCallbackRequest {

    @NotBlank
    @Schema(description = "Correlation ID of the original order", example = "c0ffee-1234")
    private String correlationId;

    /** "COMPLETED" or "FAILED" - anything else is treated as FAILED. */
    @NotBlank
    @Schema(description = "Outcome; anything other than COMPLETED is treated as FAILED",
            allowableValues = {"COMPLETED", "FAILED"}, example = "COMPLETED")
    private String status;

    @Schema(description = "Optional detail from Service B", example = "risk check passed")
    private String message;
}
