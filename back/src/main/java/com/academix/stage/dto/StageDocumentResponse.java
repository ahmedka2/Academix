package com.academix.stage.dto;

import com.academix.stage.entity.StageDocumentStatus;
import com.academix.stage.entity.StageDocumentType;

import java.time.Instant;

public record StageDocumentResponse(
		Long id,
		Long studentId,
		String studentName,
		String studentIdentifier,
		StageDocumentType type,
		StageDocumentStatus status,
		String comment,
		Instant uploadedAt,
		Instant reviewedAt,
		String reviewedBy) {
}
