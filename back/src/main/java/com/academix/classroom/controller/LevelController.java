package com.academix.classroom.controller;

import com.academix.classroom.dto.LevelRequest;
import com.academix.classroom.dto.LevelResponse;
import com.academix.classroom.service.LevelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/levels")
public class LevelController {

	private final LevelService levelService;

	public LevelController(LevelService levelService) {
		this.levelService = levelService;
	}

	@GetMapping
	public ResponseEntity<List<LevelResponse>> getAll() {
		return ResponseEntity.ok(levelService.getAll());
	}

	@PostMapping
	public ResponseEntity<LevelResponse> create(@Valid @RequestBody LevelRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(levelService.create(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<LevelResponse> update(@PathVariable Long id, @Valid @RequestBody LevelRequest request) {
		return ResponseEntity.ok(levelService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		levelService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
