package com.academix.auth.dto;

public record AuthResponse(String message, UserResponse user, String token, String tokenType) {
}
