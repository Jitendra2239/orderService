package com.jitendra.orderservice.service;

import com.jitendra.orderservice.dto.CartDto;
import com.jitendra.orderservice.dto.CheckoutItem;
import com.jitendra.orderservice.dto.CheckoutRequest;
import com.jitendra.orderservice.dto.CheckoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CheckoutService {
 private  final CartAndAdress cartAndAdress;
 public CheckoutResponse checkout(
        CheckoutRequest request) {

     CartDto cart =
             cartAndAdress.getCart(request.getUserId());

     if (cart.getItems().isEmpty()) {
         throw new RuntimeException("Cart empty");
     }

     boolean valid = cartAndAdress.validateStock(
             cart.getItems());
     if (!valid) {
         throw new RuntimeException(
                 "Insufficient stock");
     }
     List<CheckoutItem> items =
             mapItems(cart);

     Double subtotal =
             calculateSubtotal(items);

     CheckoutResponse response =
             new CheckoutResponse();

     response.setItems(items);
     response.setSubtotal(subtotal);

     // no coupon
     response.setDiscount(0.0);

     // simple fixed tax example
     Double tax =(subtotal*0.18);

     response.setTax(tax);

     response.setGrandTotal(tax);

     return response;

 }
    private Double calculateSubtotal(
            List<CheckoutItem> items){

        return items.stream()
                .map(CheckoutItem::getLineTotal)
                .reduce(
                        0.0,
                        (x,y)->x+y
                );
    }
    private List<CheckoutItem> mapItems(
            CartDto cart){

        return cart.getItems()
                .stream()
                .map(item -> new CheckoutItem(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getQuantity()
                ))
                .toList();
    }
}