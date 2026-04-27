package com.jitendra.orderservice.dto;

import lombok.Data;

@Data
public class CheckoutItem {

    private String productId;

    private String productName;

    private Integer quantity;

    private Double unitPrice;

    private Double lineTotal;

    private Integer availableStock;

    public CheckoutItem() {
    }

    public CheckoutItem(String productId,
                        String productName,
                        Integer quantity,
                        Double unitPrice,
                        Integer availableStock) {

        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.availableStock = availableStock;

        this.lineTotal =
             unitPrice.valueOf(quantity);
    }

    // getters setters
}