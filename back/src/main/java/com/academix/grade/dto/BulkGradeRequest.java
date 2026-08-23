package com.academix.grade.dto;

import com.academix.grade.entity.Semester;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record BulkGradeRequest(
		@NotNull(message = "Class is required") Long classId,
		@NotBlank(message = "Subject is required")
		@Size(max = 100, message = "Subject must be at most 100 characters") String subject,
		@NotNull(message = "Coefficient is required")
		@DecimalMin(value = "0.1", message = "Coefficient must be at least 0.1")
		@DecimalMax(value = "10", message = "Coefficient must be at most 10") BigDecimal coefficient,
		@NotNull(message = "Semester is required") Semester semester,
		@NotBlank(message = "Academic year is required")
		@Pattern(regexp = "\\d{4}-\\d{4}", message = "Academic year must be in the format YYYY-YYYY") String academicYear,
		@NotEmpty(message = "At least one entry is required") @Valid List<BulkGradeEntry> entries) {
}
