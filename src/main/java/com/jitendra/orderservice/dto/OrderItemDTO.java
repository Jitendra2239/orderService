package com.jitendra.orderservice.dto;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class OrderItemDTO {

    private String productId;
    private Integer quantity;
    private Double price;

    // getters and setters
}