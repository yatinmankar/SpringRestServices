package com.example.serviceb.dto;

public record CallbackRequest(String correlationId, String status, String message) {
}
