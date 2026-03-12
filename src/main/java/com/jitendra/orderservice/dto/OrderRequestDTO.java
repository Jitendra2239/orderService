package com.jitendra.orderservice.dto;

import lombok.Data;

import java.util.List;
@Data
public class OrderRequestDTO {

    private Long userId;
    private String shippingAddress;
    private List<OrderItemDTO> items;

    // getters and setters
}