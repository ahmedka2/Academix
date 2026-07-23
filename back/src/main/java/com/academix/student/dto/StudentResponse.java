package com.academix.student.dto;

import java.time.Instant;

public record StudentResponse(
		Long id,
		Long userId,
		String firstName,
		String lastName,
		String cin,
		String studentIdentifier,
		String email,
		String phone,
		String fieldOfStudy,
		Long classId,
		String className,
		String levelName,
		String photoUrl,
		String address,
		Instant createdAt,
		Instant updatedAt) {
}
