package com.jitendra.orderservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class CheckoutResponse {
    private List<CheckoutItem> items;
    private Double subtotal;
    private Double discount;
    private Double tax;
    private Double shippingCharge;
    private Double grandTotal;
    private String checkoutSessionId;
}