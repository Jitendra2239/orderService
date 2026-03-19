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
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public OrderResponseDTO createOrder(OrderRequestDTO request,long userId) {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setShippingAddress(request.getShippingAddress());
        order.setOrderStatus("CREATED");

        List<OrderItem> items = new ArrayList<>();
        Double totalAmount = 0.0;

        for (OrderItemDTO itemDTO : request.getItems()) {

            OrderItem item = new OrderItem();
            item.setProductId(itemDTO.getProductId());
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(itemDTO.getPrice());

            Double totalPrice =
                    (itemDTO.getPrice())*(itemDTO.getQuantity());

            item.setTotalPrice(totalPrice);
            item.setOrder(order);

            totalAmount = totalAmount+totalPrice;

            items.add(item);
        }

        order.setOrderItems(items);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        String key = "user:" + userId;

        UserCreatedEvent user = (UserCreatedEvent) redisTemplate.opsForValue().get(key);
        OrderCreatedEvent event=new OrderCreatedEvent();
        event.setOrderId(savedOrder.getOrder_id());
        List<OrderItemDto> orderItems = new ArrayList<>();
        OrderItemDto itemDto = new OrderItemDto();
               savedOrder.getOrderItems().forEach(item->{
                   itemDto.setProductId(item.getProductId());
                   item.setQuantity(item.getQuantity());
                   orderItems.add(itemDto);
               });
        event.setItems(orderItems);
        event.setTotalAmount(totalAmount);
        event.setUserId(userId);
        event.setEmail(user.getEmail());
        event.setFirstName(user.getName());
        event.setPhone(user.getPhone());
        event.setStatus("CREATED");
        kafkaTemplate.send("order-created", savedOrder.getOrder_id().toString(), event);
        NotificationEvent event1 = new NotificationEvent();
        event1.setUserId(order.getUserId());
       // event.setEmail(order.getUserId());
        event1.setType("ORDER_PLACED");
        event1.setMessage("Your order is placed!");

        kafkaTemplate.send("notification-topic", event1);
        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(savedOrder.getOrder_id());
        response.setUserId(savedOrder.getUserId());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setOrderStatus(savedOrder.getOrderStatus());


        return response;
    }

    @Override
    public OrderResponseDTO getOrderById(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id " + orderId));

        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(order.getOrder_id());
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
            dto.setOrderId(order.getOrder_id());
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
        if (order.getOrderStatus().equals("SHIPPED") || order.getOrderStatus().equals("DELIVERED")) {
            throw new RuntimeException("Cannot cancel order at this stage");
        }
        order.setOrderStatus("CANCELLED");
        orderRepository.save(order);

        List<OrderItemDto> itemDtos = order.getOrderItems().stream()
                .map(item -> {
                    OrderItemDto dto = new OrderItemDto();
                    dto.setProductId(item.getProductId());
                    dto.setQuantity(item.getQuantity());
                    return dto;
                })
                .toList();

        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(order.getOrder_id());
        event.setUserId(order.getUserId());
        event.setItems(itemDtos);

        kafkaTemplate.send("order-cancelled", event);


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
            order.setOrderStatus(event.getReason());

        orderRepository.save(order);
        }
    }




    @KafkaListener(topics = "user-created")
    public void handleUserCreated(UserCreatedEvent event) {

        String key = "user:" + event.getUserId();

        redisTemplate.opsForValue().set(key, event);
    }
    @KafkaListener(topics = "user-updated")
    public void handleUserUpdated(UserUpdatedEvent event) {

        String key = "user:" + event.getUserId();

       UserCreatedEvent existing =(UserCreatedEvent) redisTemplate.opsForValue().get(key);



        existing.setUserId(event.getUserId());
        existing.setName(event.getName());
        existing.setEmail(event.getEmail());
        existing.setAddressLine(event.getAddressLine());
        existing .setVersion(event.getVersion());

        redisTemplate.opsForValue().set(key,existing );
    }

}
