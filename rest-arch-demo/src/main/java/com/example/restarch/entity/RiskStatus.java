package com.example.restarch.entity;

public enum RiskStatus {
    /** Risk check submitted (or about to be); no verdict yet. */
    PENDING,
    APPROVED,
    REJECTED,
    /** The risk service could not be reached when submitting - no verdict will arrive. */
    UNAVAILABLE
}
