package com.jitendra.orderservice.dto;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class OrderItemDTO {

    private Long productId;
    private Integer quantity;
    private BigDecimal price;

    // getters and setters
}