package com.academix.classroom.controller;

import com.academix.classroom.dto.AssignTeacherRequest;
import com.academix.classroom.dto.ClassRequest;
import com.academix.classroom.dto.ClassResponse;
import com.academix.classroom.dto.MyClassAssignmentResponse;
import com.academix.classroom.dto.TeacherAssignmentResponse;
import com.academix.classroom.service.SchoolClassService;
import com.academix.student.dto.StudentResponse;
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
@RequestMapping("/api/classes")
public class ClassController {

	private final SchoolClassService schoolClassService;

	public ClassController(SchoolClassService schoolClassService) {
		this.schoolClassService = schoolClassService;
	}

	@GetMapping
	public ResponseEntity<List<ClassResponse>> getAll() {
		return ResponseEntity.ok(schoolClassService.getAll());
	}

	@GetMapping("/mine")
	public ResponseEntity<List<MyClassAssignmentResponse>> getMine() {
		return ResponseEntity.ok(schoolClassService.getMine());
	}

	@GetMapping("/{id}/students")
	public ResponseEntity<List<StudentResponse>> getRoster(@PathVariable Long id) {
		return ResponseEntity.ok(schoolClassService.getRoster(id));
	}

	@PostMapping
	public ResponseEntity<ClassResponse> create(@Valid @RequestBody ClassRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(schoolClassService.create(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ClassResponse> update(@PathVariable Long id, @Valid @RequestBody ClassRequest request) {
		return ResponseEntity.ok(schoolClassService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		schoolClassService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/assignments")
	public ResponseEntity<TeacherAssignmentResponse> assignTeacher(@PathVariable Long id, @Valid @RequestBody AssignTeacherRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(schoolClassService.assignTeacher(id, request));
	}

	@DeleteMapping("/{id}/assignments/{assignmentId}")
	public ResponseEntity<Void> unassignTeacher(@PathVariable Long id, @PathVariable Long assignmentId) {
		schoolClassService.unassignTeacher(id, assignmentId);
		return ResponseEntity.noContent().build();
	}
}
