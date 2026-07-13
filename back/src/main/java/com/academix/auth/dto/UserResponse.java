package com.academix.auth.dto;


public record UserResponse(Long id, String fullName, String email, Integer role) {
}
