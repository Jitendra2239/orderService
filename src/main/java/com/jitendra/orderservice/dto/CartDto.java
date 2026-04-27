package com.jitendra.orderservice.dto;



import java.util.List;

public class CartDto {

    private Long userId;

    private List<CartItem> items;

    private Double totalAmount;

    public CartDto() {
    }



    public Long getUserId() {
        return userId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }



    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}

