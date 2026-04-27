package com.jitendra.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;


    @Data
    @Builder
    public class AddressDto {
        private String street;
        private String city;
        private String state;
        private String pincode;
        private String country;

    private Boolean isDefault;
}
