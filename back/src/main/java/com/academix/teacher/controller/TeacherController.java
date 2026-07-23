package com.academix.teacher.controller;

import com.academix.teacher.dto.CreateTeacherRequest;
import com.academix.teacher.dto.TeacherRequest;
import com.academix.teacher.dto.TeacherResponse;
import com.academix.teacher.service.TeacherService;
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
@RequestMapping("/api/teachers")
public class TeacherController {

	private final TeacherService teacherService;

	public TeacherController(TeacherService teacherService) {
		this.teacherService = teacherService;
	}

	@GetMapping
	public ResponseEntity<List<TeacherResponse>> getAllTeachers() {
		return ResponseEntity.ok(teacherService.getAllTeachers());
	}

	@GetMapping("/{id}")
	public ResponseEntity<TeacherResponse> getTeacher(@PathVariable Long id) {
		return ResponseEntity.ok(teacherService.getTeacher(id));
	}

	@PostMapping
	public ResponseEntity<TeacherResponse> createTeacher(@Valid @RequestBody CreateTeacherRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.createTeacher(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<TeacherResponse> updateTeacher(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
		return ResponseEntity.ok(teacherService.updateTeacher(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
		teacherService.deleteTeacher(id);
		return ResponseEntity.noContent().build();
	}
}
