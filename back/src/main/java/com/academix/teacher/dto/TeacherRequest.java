package com.academix.teacher.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeacherRequest(
		@NotBlank(message = "Full name is required")
		@Size(max = 200, message = "Full name must be at most 200 characters") String fullName,
		@Email(message = "Email must be valid")
		@NotBlank(message = "Email is required")
		@Size(max = 255, message = "Email must be at most 255 characters") String email) {
}
