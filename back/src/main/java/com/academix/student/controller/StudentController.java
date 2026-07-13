package com.academix.student.controller;

import com.academix.student.dto.CreateStudentRequest;
import com.academix.student.dto.StudentRequest;
import com.academix.student.dto.StudentResponse;
import com.academix.student.service.StudentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

	private final StudentService studentService;

	public StudentController(StudentService studentService) {
		this.studentService = studentService;
	}

	@GetMapping
	public ResponseEntity<List<StudentResponse>> getAllStudents() {
		return ResponseEntity.ok(studentService.getAllStudents());
	}

	@GetMapping("/search")
	public ResponseEntity<List<StudentResponse>> searchStudents(@RequestParam(name = "query", required = false) String query) {
		return ResponseEntity.ok(studentService.searchStudents(query));
	}

	@GetMapping("/me")
	public ResponseEntity<StudentResponse> getCurrentStudent() {
		return ResponseEntity.ok(studentService.getCurrentStudent());
	}

	@GetMapping("/{id}")
	public ResponseEntity<StudentResponse> getStudent(@PathVariable Long id) {
		return ResponseEntity.ok(studentService.getStudent(id));
	}

	@PostMapping
	public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody CreateStudentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<StudentResponse> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
		return ResponseEntity.ok(studentService.updateStudent(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
		studentService.deleteStudent(id);
		return ResponseEntity.noContent().build();
	}
}
