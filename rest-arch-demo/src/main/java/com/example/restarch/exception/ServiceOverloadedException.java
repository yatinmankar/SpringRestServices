package com.example.restarch.exception;

public class ServiceOverloadedException extends RuntimeException {
    public ServiceOverloadedException(String message) {
        super(message);
    }
}
