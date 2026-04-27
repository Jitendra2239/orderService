package com.jitendra.orderservice.repository;


import com.jitendra.orderservice.dto.OrderProjection;
import com.jitendra.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByEmail(String email);

    Optional<OrderProjection>  findProjectedByOrderId(Long orderId);
}