package com.jitendra.orderservice.service;

import com.jitendra.orderservice.dto.AddressDto;
import com.jitendra.orderservice.dto.CartDto;
import com.jitendra.orderservice.dto.CartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CartAndAdress {
    private  final RestTemplate restTemplate;
    @Value("${user.service.url}")
    private String userServiceUrl;
    @Value("${cart.service.url}")
    private String cartServiceUrl;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;
    public List<AddressDto> getAddress(Long userId) {

        String url = userServiceUrl + "/internal/users/" + userId;

        ResponseEntity<List<AddressDto>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<AddressDto>>() {}
                );

        return response.getBody();
    }

    public CartDto getCart(Long userId) {

        String url = cartServiceUrl+"/"+userId;

        ResponseEntity<CartDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null ,
                CartDto.class
        );

        return response.getBody();
    }
    public  boolean validateStock(List<CartItem> items){
        String url = cartServiceUrl + "/check";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<CartItem>> request =
                new HttpEntity<>(items, headers);

        ResponseEntity<Boolean> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        Boolean.class
                );

        return Boolean.TRUE.equals(response.getBody());
    }
}
