package com.academix.classroom.dto;

public record MyClassAssignmentResponse(
		Long assignmentId,
		Long classId,
		String className,
		String levelName,
		String subject,
		int studentCount) {
}
