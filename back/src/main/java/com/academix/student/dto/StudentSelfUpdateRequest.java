package com.academix.student.dto;

import jakarta.validation.constraints.Size;

public record StudentSelfUpdateRequest(
		@Size(max = 30, message = "Phone must be at most 30 characters") String phone) {
}
