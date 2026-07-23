package com.academix.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssignTeacherRequest(
		@NotNull(message = "Teacher is required") Long teacherId,
		@NotBlank(message = "Subject is required")
		@Size(max = 100, message = "Subject must be at most 100 characters") String subject) {
}
