package com.jitendra.orderservice.dto;

import java.util.List;

public interface OrderProjection {

    String getPaymentStatus();

    String getShippingAddress();

    String getOrderStatus();

    List<OrderItemProjection> getOrderItems();
}
