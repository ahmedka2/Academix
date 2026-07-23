package com.academix.statistics.dto;

public record ClassAverageResponse(Long classId, String className, String levelName, int studentCount, double average) {
}
