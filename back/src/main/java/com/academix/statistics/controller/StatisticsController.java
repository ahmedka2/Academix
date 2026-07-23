package com.academix.statistics.controller;

import com.academix.statistics.dto.StatisticsResponse;
import com.academix.statistics.service.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

	private final StatisticsService statisticsService;

	public StatisticsController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping
	public ResponseEntity<StatisticsResponse> getStatistics() {
		return ResponseEntity.ok(statisticsService.getStatistics());
	}
}
