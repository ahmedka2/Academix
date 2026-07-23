package com.academix.classroom.dto;

import java.util.List;

public record ClassResponse(
		Long id,
		String name,
		Long levelId,
		String levelName,
		int studentCount,
		int capacity,
		List<TeacherAssignmentResponse> teacherAssignments) {
}
