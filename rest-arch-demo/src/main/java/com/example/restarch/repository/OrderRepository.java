package com.example.restarch.repository;

import com.example.restarch.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    Optional<Order> findByCorrelationId(String correlationId);

    Optional<Order> findByIdAndClientId(String id, String clientId);

    List<Order> findByClientIdOrderByCreatedAtDesc(String clientId);

    Page<Order> findByClientId(String clientId, Pageable pageable);
}
