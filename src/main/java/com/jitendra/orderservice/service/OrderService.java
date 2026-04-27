package com.jitendra.orderservice.service;



import com.jitendra.orderservice.dto.AddressDto;
import com.jitendra.orderservice.dto.OrderProjection;
import com.jitendra.orderservice.dto.OrderRequestDTO;
import com.jitendra.orderservice.dto.OrderResponseDTO;
import com.jitendra.orderservice.model.Order;

import java.util.List;

public interface OrderService {

    OrderResponseDTO createOrder(Long userId, String email, AddressDto address);

    OrderResponseDTO getOrderById(Long orderId,String email);
    public Order getOrderById(Long orderId);
    List<OrderResponseDTO> getAllOrders();
    public List<OrderResponseDTO> getOrdersByUser(String email);
    void cancelOrder(Long orderId,String email);
    public OrderProjection getOrderDetails(Long orderId);
}