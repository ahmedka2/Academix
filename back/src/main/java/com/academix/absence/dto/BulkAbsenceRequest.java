package com.academix.absence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record BulkAbsenceRequest(
		@NotNull(message = "Class is required") Long classId,
		@NotBlank(message = "Subject is required")
		@Size(max = 100, message = "Subject must be at most 100 characters") String subject,
		@NotNull(message = "Date is required") LocalDate date,
		List<Long> absentStudentIds) {
}
