package com.jitendra.orderservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class OrderResponseDTO {

    private Long orderId;
    private Long userId;
    private Double totalAmount;
    private String orderStatus;
    private List<OrderItemDTO> items;

    // getters and setters
}