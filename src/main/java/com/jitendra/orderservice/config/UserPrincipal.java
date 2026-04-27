package com.jitendra.orderservice.config;

public class UserPrincipal {

    private String userId;
    private String email;

    public UserPrincipal(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return email;
    }
}