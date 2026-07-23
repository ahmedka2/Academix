package com.academix.teacher.dto;

import java.time.Instant;

public record TeacherResponse(
		Long id,
		String fullName,
		String email,
		Instant createdAt,
		Instant updatedAt) {
}
