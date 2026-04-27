package com.jitendra.orderservice.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private Long userId;
    private Long shippingAddressId;
    private String coupon;
}