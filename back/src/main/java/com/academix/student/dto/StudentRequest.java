package com.academix.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentRequest(
		@NotBlank(message = "First name is required")
		@Size(max = 100, message = "First name must be at most 100 characters") String firstName,
		@NotBlank(message = "Last name is required")
		@Size(max = 100, message = "Last name must be at most 100 characters") String lastName,
		@NotBlank(message = "CIN is required")
		@Size(max = 50, message = "CIN must be at most 50 characters") String cin,
		@NotBlank(message = "Student identifier is required")
		@Size(max = 50, message = "Student identifier must be at most 50 characters") String studentIdentifier,
		@Email(message = "Email must be valid")
		@NotBlank(message = "Email is required")
		@Size(max = 255, message = "Email must be at most 255 characters") String email,
		@Size(max = 30, message = "Phone must be at most 30 characters") String phone,
		@NotBlank(message = "Field of study is required")
		@Size(max = 100, message = "Field of study must be at most 100 characters") String fieldOfStudy,
		Long classId,
		@Size(max = 500, message = "Photo URL must be at most 500 characters") String photoUrl,
		@Size(max = 500, message = "Address must be at most 500 characters") String address) {
}
