package com.academix.absence.dto;

import com.academix.absence.entity.AbsenceStatus;

import java.time.Instant;
import java.time.LocalDate;

public record AbsenceResponse(
		Long id,
		Long studentId,
		String studentName,
		String studentIdentifier,
		String subject,
		LocalDate date,
		String comment,
		String justification,
		AbsenceStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
