package com.jitendra.orderservice.service;



import com.jitendra.event.*;
import com.jitendra.orderservice.dto.OrderItemDTO;
import com.jitendra.orderservice.dto.OrderRequestDTO;
import com.jitendra.orderservice.dto.OrderResponseDTO;

import com.jitendra.orderservice.exception.BadRequestException;
import com.jitendra.orderservice.exception.ResourceNotFoundException;
import com.jitendra.orderservice.model.Order;
import com.jitendra.orderservice.model.OrderItem;
import com.jitendra.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String,Object> kafkaTemplate;


    @Override
    public OrderResponseDTO createOrder(OrderRequestDTO request) {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setShippingAddress(request.getShippingAddress());
        order.setOrderStatus("CREATED");

        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemDTO itemDTO : request.getItems()) {

            OrderItem item = new OrderItem();
            item.setProductId(itemDTO.getProductId());
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(itemDTO.getPrice());

            BigDecimal totalPrice =
                    itemDTO.getPrice().multiply(
                            BigDecimal.valueOf(itemDTO.getQuantity()));

            item.setTotalPrice(totalPrice);
            item.setOrder(order);

            totalAmount = totalAmount.add(totalPrice);

            items.add(item);
        }

        order.setOrderItems(items);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(savedOrder.getId());
        response.setUserId(savedOrder.getUserId());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setOrderStatus(savedOrder.getOrderStatus());
        OrderCreatedEvent event=new OrderCreatedEvent();
        event.setOrderId(response.getOrderId());
        event.setQuantity(10);
        event.setProductId(78l);
        publishOrderEvent(event);
        return response;
    }

    @Override
    public OrderResponseDTO getOrderById(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id " + orderId));

        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(order.getId());
        response.setUserId(order.getUserId());
        response.setTotalAmount(order.getTotalAmount());
        response.setOrderStatus(order.getOrderStatus());

        return response;
    }

    @Override
    public List<OrderResponseDTO> getAllOrders() {

        List<Order> orders = orderRepository.findAll();

        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("No orders found");
        }

        List<OrderResponseDTO> responses = new ArrayList<>();

        for (Order order : orders) {

            OrderResponseDTO dto = new OrderResponseDTO();
            dto.setOrderId(order.getId());
            dto.setUserId(order.getUserId());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setOrderStatus(order.getOrderStatus());

            responses.add(dto);
        }

        return responses;
    }

    @Override
    public void cancelOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id " + orderId));

        order.setOrderStatus("CANCELLED");

        orderRepository.save(order);
    }
    public void publishOrderEvent(OrderCreatedEvent event){

        kafkaTemplate.send("order-created", event);

    }
    @KafkaListener(topics = "payment-success", groupId = "order-group")
    public void consumePaymentSuccess(PaymentSuccessEvent event) {

        System.out.println("Payment successful for order: " + event.getOrderId());

        Long order1= event.getOrderId();
        Optional<Order>order2=orderRepository.findById(order1);
        if(order2.isPresent()) {
            Order    order = order2.get();
            order.setOrderStatus("CONFIRMED");
            orderRepository.save(order);
        }

    }

    @KafkaListener(topics = "payment-failed", groupId = "order-group")
    public void consumePaymentFailed(PaymentFailedEvent event) {

        System.out.println("Payment failed for order: " + event.getOrderId());
        Long order1= event.getOrderId();
        Optional<Order>order2=orderRepository.findById(order1);
        if(order2.isPresent()) {
            Order      order = order2.get();
            order.setOrderStatus("PAYMENT_FAILED\"");
            orderRepository.save(order);
        }

    }

    @KafkaListener(topics = "inventory-failed", groupId = "order-group")
    public void consumeInventoryFailed(InventoryFailedEvent event) {

        System.out.println("Inventory failed for order: " + event.getOrderId());
        Long order1= event.getOrderId();
        Optional<Order>order2=orderRepository.findById(order1);
        if(order2.isPresent()) {
            Order order = order2.get();
            order.setOrderStatus("Product not Avilabel");

        orderRepository.save(order);
        }
    }
}
