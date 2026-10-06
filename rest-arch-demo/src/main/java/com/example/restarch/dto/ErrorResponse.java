package com.example.restarch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Standard error payload")
public class ErrorResponse {

    @Schema(description = "When the error was produced")
    private Instant timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Short error label", example = "validation failed")
    private String error;

    @Schema(description = "Per-field or per-cause detail", example = "[\"quantity: must be greater than 0\"]")
    private List<String> details;
}
