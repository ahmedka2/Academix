package com.academix.document.dto;

import com.academix.document.entity.DocumentType;
import com.academix.grade.entity.Semester;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record GenerateDocumentRequest(
		@NotNull(message = "Student is required") Long studentId,
		@NotNull(message = "Document type is required") DocumentType type,
		@NotBlank(message = "Academic year is required")
		@Pattern(regexp = "\\d{4}-\\d{4}", message = "Academic year must be in the format YYYY-YYYY") String academicYear,
		Semester semester) {
}
