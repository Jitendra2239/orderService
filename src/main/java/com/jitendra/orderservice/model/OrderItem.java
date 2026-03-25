package com.jitendra.orderservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
@Data
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productId;

    private Integer quantity;

    private Double price;

    private Double totalPrice;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // getters and setters
}