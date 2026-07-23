package com.academix.teacher.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeacherRequest(
		@NotBlank(message = "Full name is required")
		@Size(max = 200, message = "Full name must be at most 200 characters") String fullName,
		@Email(message = "Email must be valid")
		@NotBlank(message = "Email is required")
		@Size(max = 255, message = "Email must be at most 255 characters") String email,
		@NotBlank(message = "Password is required")
		@Size(min = 8, max = 200, message = "Password must be between 8 and 200 characters") String password) {
}
