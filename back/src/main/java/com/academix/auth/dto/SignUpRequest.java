package com.academix.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
		@NotBlank(message = "Full name is required") String fullName,
		@Email(message = "Email must be valid")
		@NotBlank(message = "Email is required") String email,
		@NotBlank(message = "Password is required")
		@Size(min = 8, max = 200, message = "Password must be between 8 and 200 characters") String password,
		@Min(value = 0, message = "Role must be 0, 1, or 2")
		@Max(value = 2, message = "Role must be 0, 1, or 2") Integer role) {
}
