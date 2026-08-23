package com.academix.grade.dto;

import com.academix.grade.entity.GradeStatus;
import com.academix.grade.entity.Semester;

import java.math.BigDecimal;
import java.time.Instant;

public record GradeResponse(
		Long id,
		Long studentId,
		String studentName,
		String studentIdentifier,
		String subject,
		BigDecimal score,
		BigDecimal coefficient,
		Semester semester,
		String academicYear,
		GradeStatus status,
		String submittedBy,
		String reviewComment,
		Instant reviewedAt,
		String reviewedBy) {
}
