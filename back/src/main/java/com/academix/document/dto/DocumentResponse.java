package com.academix.document.dto;

import com.academix.document.entity.DocumentType;
import com.academix.grade.entity.Semester;

import java.time.Instant;

public record DocumentResponse(
		Long id,
		Long studentId,
		String studentName,
		String studentIdentifier,
		DocumentType type,
		String academicYear,
		Semester semester,
		Instant generatedAt,
		String generatedBy) {
}
