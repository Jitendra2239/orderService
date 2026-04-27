package com.jitendra.orderservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Data
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private Long userId;
    private String email;
    private String trackingId;
    private Double totalAmount;

    private String orderStatus;

    private String paymentStatus;

    private String shippingAddress;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private String cancelStatus;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

    // getters and setters
}