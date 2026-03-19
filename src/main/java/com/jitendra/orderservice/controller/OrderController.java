package com.jitendra.orderservice.controller;



import com.jitendra.orderservice.config.JwtUtil;
import com.jitendra.orderservice.dto.OrderRequestDTO;
import com.jitendra.orderservice.dto.OrderResponseDTO;
import com.jitendra.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/order")
public class OrderController {

    private final OrderService orderService;
  private  final JwtUtil jwtUtil;


    @PostMapping("/save")
    public ResponseEntity<OrderResponseDTO> createOrder(
            @RequestBody OrderRequestDTO request,@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7); // remove "Bearer "

        Long userId = jwtUtil.extractUserId(jwt);
        return ResponseEntity.ok(orderService.createOrder(request,userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {

        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelOrder(
            @PathVariable Long id) {

        orderService.cancelOrder(id);

        return ResponseEntity.ok("Order cancelled successfully");
    }
}