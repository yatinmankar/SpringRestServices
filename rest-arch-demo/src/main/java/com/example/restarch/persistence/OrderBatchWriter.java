package com.example.restarch.persistence;

import com.example.restarch.entity.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persists a batch of orders through Hibernate (not raw JDBC) so that
 * Envers picks up the creation as an audited revision, while still getting
 * JDBC-level batching via hibernate.jdbc.batch_size + a single flush/clear
 * per batch instead of a round trip per row.
 */
@Component
public class OrderBatchWriter {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void persistBatch(List<Order> orders) {
        for (Order order : orders) {
            entityManager.persist(order);
        }
        entityManager.flush();
        entityManager.clear();
    }
}
