package com.academix.absence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AbsenceRequest(
		@NotNull(message = "Student is required") Long studentId,
		@NotBlank(message = "Subject is required")
		@Size(max = 100, message = "Subject must be at most 100 characters") String subject,
		@NotNull(message = "Date is required") LocalDate date,
		@Size(max = 500, message = "Comment must be at most 500 characters") String comment) {
}
