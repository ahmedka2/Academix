package com.academix.grade.dto;

import com.academix.grade.entity.Semester;

import java.math.BigDecimal;

public record GradeResponse(
		Long id,
		Long studentId,
		String studentName,
		String studentIdentifier,
		String subject,
		BigDecimal score,
		BigDecimal coefficient,
		Semester semester,
		String academicYear) {
}
