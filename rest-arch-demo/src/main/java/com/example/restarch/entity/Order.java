package com.example.restarch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @Audited (Hibernate Envers) records every INSERT/UPDATE to this entity in a
 * shadow orders_AUD table plus a shared REVINFO table (revision number +
 * timestamp), giving a full history of every status transition - who/what
 * changed it and when - without hand-rolling an audit log. @CreatedDate /
 * @LastModifiedDate (Spring Data JPA auditing) additionally stamp the
 * current-row timestamps for cheap "when was this last touched" queries
 * without joining to the audit table.
 *
 * correlationId is unique: exactly one order is created per inbound request,
 * and it's how Service B's callback finds its way back to this row.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_client_id", columnList = "client_id"),
        @Index(name = "idx_orders_correlation_id", columnList = "correlation_id", unique = true)
})
@Audited
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    private String id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "correlation_id", nullable = false, unique = true)
    private String correlationId;

    @Column(name = "instrument_id", nullable = false)
    private String instrumentId;

    private int quantity;

    private BigDecimal price;

    private String side;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /** Verdict from the risk service; arrives asynchronously after the order is created. Null on rows from before this column existed. */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_status")
    @Builder.Default
    private RiskStatus riskStatus = RiskStatus.PENDING;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "risk_message")
    private String riskMessage;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Optimistic locking - guards against the batch writer and a callback racing to update the same row. */
    @Version
    private Long version;
}
