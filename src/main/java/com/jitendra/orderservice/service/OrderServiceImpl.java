package com.jitendra.orderservice.service;



import com.jitendra.event.*;
import com.jitendra.orderservice.dto.*;

import com.jitendra.orderservice.exception.BadRequestException;
import com.jitendra.orderservice.exception.OrderNotFoundException;
import com.jitendra.orderservice.exception.ResourceNotFoundException;
import com.jitendra.orderservice.model.Order;
import com.jitendra.orderservice.model.OrderItem;
import com.jitendra.orderservice.model.OrderStatus;
import com.jitendra.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private final CartAndAdress cartAndAdress;

    @Override
    public OrderResponseDTO createOrder(Long userId, String email,AddressDto address) {


        CartDto cart = cartAndAdress.getCart(userId);

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }



        System.out.println("email->cart------>"+cart);
        if (address == null) {
            throw new BadRequestException("Address not found");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setShippingAddress(address.getStreet()+" "+address.getCity()+" "+address.getState()+" "+address.getCountry()+" "+address.getPincode());
        order.setOrderStatus("CREATED");

        List<OrderItem> items = new ArrayList<>();
        double totalAmount = 0.0;

        for (CartItem item : cart.getItems()) {

            if (item == null) continue;

            double price = item.getPrice() == null ? 0.0 : item.getPrice();
            int qty = item.getQuantity() == null ? 0 : item.getQuantity();

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(qty);
            orderItem.setPrice(price);

            double totalPrice = price * qty;

            orderItem.setTotalPrice(totalPrice);
            orderItem.setOrder(order);

            totalAmount += totalPrice;
            items.add(orderItem);
        }

        order.setOrderItems(items);
        order.setTotalAmount(totalAmount);

        // 🔹 4. Save Order
        Order savedOrder = orderRepository.save(order);

        // 🔹 5. Kafka Event (FIXED)
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(savedOrder.getOrderId());
        event.setUserId(userId);
        event.setEmail(email);

        event.setTotalAmount(totalAmount);
        event.setStatus("CREATED");

        List<OrderItemEvent> orderItems = new ArrayList<>();

        for (OrderItem item : savedOrder.getOrderItems()) {
            OrderItemEvent itemEvent = new OrderItemEvent(); // ✅ NEW object each time
            itemEvent.setProductId(item.getProductId());
            itemEvent.setQuantity(item.getQuantity());

            orderItems.add(itemEvent);
        }

        event.setItems(orderItems);

        kafkaTemplate.send("order-created",  event);

        // 🔹 6. Notification Event
        NotificationEvent notification = new NotificationEvent();
        notification.setUserId(userId);
        notification.setOrder_id(savedOrder.getOrderId());
        notification.setEmail(email);
        notification.setType("ORDER_PLACED");
        notification.setMessage("Your order is placed!");

        kafkaTemplate.send("notification-topic", notification);

        // 🔹 7. Response
        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(savedOrder.getOrderId());
        response.setUserId(savedOrder.getUserId());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setOrderStatus(savedOrder.getOrderStatus());

        return response;
    }

    @Override
    public OrderResponseDTO getOrderById(Long orderId,String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id " + orderId));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            if (!order.getEmail().equals(email)) {
                throw new AccessDeniedException("You are not allowed to cancel this order");
            }
        }



        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(order.getOrderId());
        response.setUserId(order.getUserId());
        response.setTotalAmount(order.getTotalAmount());
        response.setOrderStatus(order.getOrderStatus());

        return response;
    }

    @Override
    public Order getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id " + orderId));


      return order;
    }
@Override
    public OrderProjection getOrderDetails(Long orderId){

        return orderRepository
                .findProjectedByOrderId(orderId)
                .orElseThrow(
                        () -> new RuntimeException("Order not found")
                );
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
            dto.setOrderId(order.getOrderId());
            dto.setUserId(order.getUserId());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setOrderStatus(order.getOrderStatus());

            responses.add(dto);
        }

        return responses;
    }


    @Override
    public List<OrderResponseDTO> getOrdersByUser(String email) {

        return orderRepository.findByEmail(email)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public void cancelOrder(Long orderId, String email) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getOrderStatus().equals("SHIPPED") || order.getOrderStatus().equals("DELIVERED")) {
            throw new RuntimeException("Cannot cancel order at this stage");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // 🔒 If NOT admin → check ownership
        if (!isAdmin) {
            if (!order.getEmail().equals(email)) {
                throw new AccessDeniedException("You are not allowed to cancel this order");
            }
        }


        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Order already cancelled");
        }


        if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Delivered order cannot be cancelled");
        }


        order.setOrderStatus("CANCELLED");
        order.setCancelStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());

        orderRepository.save(order);
        List<OrderItemEvent> itemDtos = order.getOrderItems().stream()
                .map(item -> {
                    OrderItemEvent dto = new OrderItemEvent();
                    dto.setProductId(item.getProductId());
                    dto.setQuantity(item.getQuantity());
                    return dto;
                })
                .toList();

        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(order.getOrderId());
        event.setUserId(order.getUserId());
        event.setItems(itemDtos);

        kafkaTemplate.send("order-cancelled", event);
    }

    public void publishOrderEvent(OrderCreatedEvent event){

        kafkaTemplate.send("order-created", event);

    }
    private OrderResponseDTO mapToDto(Order order) {

        OrderResponseDTO dto = new OrderResponseDTO();

        dto.setOrderId(order.getOrderId());
        dto.setUserId(order.getUserId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setOrderStatus(order.getOrderStatus());

        // Map OrderItems → OrderItemDTO
        if (order.getOrderItems() != null) {
            List<OrderItemDTO> items = order.getOrderItems()
                    .stream()
                    .map(this::mapItemToDto)
                    .toList();

            dto.setItems(items);
        }

        return dto;
    }
    private OrderItemDTO mapItemToDto(OrderItem item) {

        OrderItemDTO dto = new OrderItemDTO();

        dto.setProductId(item.getProductId());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());

        return dto;
    }




    @KafkaListener(topics = "payment-success", groupId = "order-group")
    public void consumePaymentSuccess(PaymentSuccessEvent event) {

        System.out.println("Payment successful for order: " + event.getOrderId());

        System.out.println("Payment successful for order: " + event.getUserId());
        Order order=orderRepository.findById(event.getOrderId()).orElseThrow(()->new OrderNotFoundException("Order not found with id " + event.getOrderId()));
        if("CONFIRMED".equals(order.getOrderStatus()))return;
        order.setOrderStatus("CONFIRMED");
        orderRepository.save(order);


    }

    @KafkaListener(topics = "payment-failed", groupId = "order-group")
    public void consumePaymentFailed(PaymentFailedEvent event) {

        System.out.println("Payment failed for order: " + event.getOrderId());
        Order order=orderRepository.findById(event.getOrderId()).orElseThrow(()->new OrderNotFoundException("Order not found with id " + event.getOrderId()));
        order.setOrderStatus("Payment failed");
        orderRepository.save(order);

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




//    @KafkaListener(topics = "user-created")
//    public void handleUserCreated(UserCreatedEvent event) {
//
//        String key = "user:" + event.getUserId();
//
//        redisTemplate.opsForValue().set(key, event);
//    }
//    @KafkaListener(topics = "user-updated")
//    public void handleUserUpdated(UserUpdatedEvent event) {
//
//        String key = "user:" + event.getUserId();
//
//       UserCreatedEvent existing =(UserCreatedEvent) redisTemplate.opsForValue().get(key);
//
//
//
//        existing.setUserId(event.getUserId());
//        existing.setName(event.getName());
//        existing.setEmail(event.getEmail());
//        existing.setAddressLine(event.getAddressLine());
//        existing .setVersion(event.getVersion());
//
//        redisTemplate.opsForValue().set(key,existing );
//    }
    @KafkaListener(topics = "shipment-created", groupId = "order-group")
    public void handleShipmentCreated(ShipmentCreatedEvent event) {

        Order order = orderRepository.findById(event.getOrderId()).orElseThrow();

        order.setOrderStatus(OrderStatus.SHIPPED.toString());
        order.setTrackingId(event.getTrackingId());

        orderRepository.save(order);
    }

    @KafkaListener(topics = "shipment-failed", groupId = "order-group")
    public void handleShipmentFailed(ShipmentFailedEvent event) {

        Order order = orderRepository.findById(event.getOrderId()).orElseThrow();

        order.setOrderStatus(OrderStatus.SHIPPED_FAILED.toString());
        orderRepository.save(order);
    }

}
