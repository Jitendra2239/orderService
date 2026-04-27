package com.jitendra.orderservice.controller;



import com.jitendra.orderservice.config.JwtUtil;
import com.jitendra.orderservice.config.UserPrincipal;
import com.jitendra.orderservice.dto.*;
import com.jitendra.orderservice.exception.BadRequestException;
import com.jitendra.orderservice.model.Order;
import com.jitendra.orderservice.service.CartAndAdress;
import com.jitendra.orderservice.service.CheckoutService;
import com.jitendra.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final CartAndAdress cartAndAdress;
    private final OrderService orderService;
    private final CheckoutService checkoutService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @RequestBody OrderRequestDTO request,
            Authentication authentication) {


        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String userId = principal.getUserId();
        String email = principal.getEmail();
        List<AddressDto> address = cartAndAdress.getAddress(Long.parseLong(userId));

        if (address.size()==0) {
            throw new BadRequestException("Address not found");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(Long.parseLong(userId), email,address.get(0)));
    }
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponseDTO>> getMyOrders(
            Authentication authentication) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String userId = principal.getUserId();
        String email = principal.getEmail();

        return ResponseEntity.ok(orderService.getOrdersByUser(email));
    }
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrder(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(orderService.getOrderById(id, email));
    }
    @GetMapping("/orderdetails/{id}")
    public ResponseEntity<Order> getOrder(
            @PathVariable Long id) {



        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<OrderProjection> getOrderDetails(
            @PathVariable Long id) {



        return ResponseEntity.ok(orderService.getOrderDetails(id));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {

        return ResponseEntity.ok(orderService.getAllOrders());
    }
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelOrder(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();

        orderService.cancelOrder(id, email);

        return ResponseEntity.ok("Order cancelled successfully");
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(
            @RequestBody CheckoutRequest request) {

        return checkoutService.checkout(request);
    }
}