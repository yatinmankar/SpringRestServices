package com.example.restarch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * The idempotency_key column is the primary key: attempting to insert a
 * duplicate throws DataIntegrityViolationException, which is how
 * OrderService detects an in-flight or already-completed duplicate request
 * safely across multiple app instances.
 */
@Entity
@Table(name = "idempotency_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "response_status", nullable = false)
    private String responseStatus; // PROCESSING | COMPLETED

    @Lob
    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
