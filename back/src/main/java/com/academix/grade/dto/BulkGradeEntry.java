package com.academix.grade.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BulkGradeEntry(
		@NotNull(message = "Student is required") Long studentId,
		@DecimalMin(value = "0", message = "Score must be at least 0")
		@DecimalMax(value = "20", message = "Score must be at most 20") BigDecimal score) {
}
