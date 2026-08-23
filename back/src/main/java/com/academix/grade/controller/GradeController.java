package com.academix.grade.controller;

import com.academix.grade.dto.BulkGradeRequest;
import com.academix.grade.dto.GradeRequest;
import com.academix.grade.dto.GradeResponse;
import com.academix.grade.dto.ValidateGradeRequest;
import com.academix.grade.service.GradeService;
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
@RequestMapping("/api/grades")
public class GradeController {

	private final GradeService gradeService;

	public GradeController(GradeService gradeService) {
		this.gradeService = gradeService;
	}

	@GetMapping
	public ResponseEntity<List<GradeResponse>> getAll() {
		return ResponseEntity.ok(gradeService.getAll());
	}

	@GetMapping("/me")
	public ResponseEntity<List<GradeResponse>> getMine() {
		return ResponseEntity.ok(gradeService.getMine());
	}

	@PostMapping("/bulk")
	public ResponseEntity<List<GradeResponse>> createBulk(@Valid @RequestBody BulkGradeRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(gradeService.createBulk(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<GradeResponse> update(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
		return ResponseEntity.ok(gradeService.update(id, request));
	}

	@PutMapping("/{id}/validate")
	public ResponseEntity<GradeResponse> validate(@PathVariable Long id, @Valid @RequestBody ValidateGradeRequest request) {
		return ResponseEntity.ok(gradeService.validate(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		gradeService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
