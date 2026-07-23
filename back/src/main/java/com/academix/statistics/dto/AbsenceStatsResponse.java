package com.academix.statistics.dto;

public record AbsenceStatsResponse(
		long total,
		long unjustified,
		long pendingJustification,
		long justified,
		long rejected,
		double unjustifiedRate) {
}
