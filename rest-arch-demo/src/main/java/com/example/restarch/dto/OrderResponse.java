package com.example.restarch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Acknowledgement of a submitted order")
public class OrderResponse {

    @Schema(description = "Server-assigned order identifier", example = "ORD-8f3c1a")
    private String orderId;

    @Schema(description = "Correlation ID threaded through Service B and back", example = "c0ffee-1234")
    private String correlationId;

    @Schema(description = "Order status at the time of the response", example = "ACCEPTED")
    private String status;

    @Schema(description = "Human-readable detail", example = "queued for processing")
    private String message;

    @Schema(description = "True when this response replays an earlier Idempotency-Key", example = "false")
    private boolean replay;

    public OrderResponse withReplay(boolean replay) {
        return new OrderResponse(orderId, correlationId, status, message, replay);
    }
}
