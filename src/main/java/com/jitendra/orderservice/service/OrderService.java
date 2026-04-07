package com.jitendra.orderservice.service;



import com.jitendra.orderservice.dto.OrderRequestDTO;
import com.jitendra.orderservice.dto.OrderResponseDTO;

import java.util.List;

public interface OrderService {

    OrderResponseDTO createOrder(OrderRequestDTO request,String email);

    OrderResponseDTO getOrderById(Long orderId,String email);

    List<OrderResponseDTO> getAllOrders();
    public List<OrderResponseDTO> getOrdersByUser(String email);
    void cancelOrder(Long orderId,String email);
}