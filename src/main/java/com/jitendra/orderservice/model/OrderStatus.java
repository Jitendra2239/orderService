package com.jitendra.orderservice.model;

public enum OrderStatus {

    CREATED,
    CONFIRMED,
    PAYMENT_PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    SHIPPED_FAILED,
    CANCELLED_FAILED,
}