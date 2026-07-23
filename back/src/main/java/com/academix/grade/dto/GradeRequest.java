package com.academix.grade.dto;

import com.academix.grade.entity.Semester;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GradeRequest(
		@NotNull(message = "Student is required") Long studentId,
		@NotBlank(message = "Subject is required")
		@Size(max = 100, message = "Subject must be at most 100 characters") String subject,
		@NotNull(message = "Score is required")
		@DecimalMin(value = "0", message = "Score must be at least 0")
		@DecimalMax(value = "20", message = "Score must be at most 20") BigDecimal score,
		@NotNull(message = "Coefficient is required")
		@DecimalMin(value = "0.1", message = "Coefficient must be at least 0.1")
		@DecimalMax(value = "10", message = "Coefficient must be at most 10") BigDecimal coefficient,
		@NotNull(message = "Semester is required") Semester semester,
		@NotBlank(message = "Academic year is required")
		@Pattern(regexp = "\\d{4}-\\d{4}", message = "Academic year must be in the format YYYY-YYYY") String academicYear) {
}
