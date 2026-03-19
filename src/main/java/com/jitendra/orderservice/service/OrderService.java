package com.jitendra.orderservice.service;



import com.jitendra.orderservice.dto.OrderRequestDTO;
import com.jitendra.orderservice.dto.OrderResponseDTO;

import java.util.List;

public interface OrderService {

    OrderResponseDTO createOrder(OrderRequestDTO request,long userId);

    OrderResponseDTO getOrderById(Long orderId);

    List<OrderResponseDTO> getAllOrders();

    void cancelOrder(Long orderId);
}