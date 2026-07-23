package com.academix.statistics.dto;

import java.util.List;

public record StatisticsResponse(
		long totalStudents,
		long totalTeachers,
		long totalClasses,
		double overallPassRate,
		List<ClassAverageResponse> averageByClass,
		List<CategoryCountResponse> studentsByFieldOfStudy,
		List<CategoryCountResponse> studentsByLevel,
		AbsenceStatsResponse absences,
		PaymentStatsResponse payments) {
}
