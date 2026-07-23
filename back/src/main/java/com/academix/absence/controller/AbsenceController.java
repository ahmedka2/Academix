package com.academix.absence.controller;

import com.academix.absence.dto.AbsenceRequest;
import com.academix.absence.dto.AbsenceResponse;
import com.academix.absence.dto.JustifyAbsenceRequest;
import com.academix.absence.dto.ValidateAbsenceRequest;
import com.academix.absence.service.AbsenceService;
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
@RequestMapping("/api/absences")
public class AbsenceController {

	private final AbsenceService absenceService;

	public AbsenceController(AbsenceService absenceService) {
		this.absenceService = absenceService;
	}

	@GetMapping
	public ResponseEntity<List<AbsenceResponse>> getAll() {
		return ResponseEntity.ok(absenceService.getAll());
	}

	@GetMapping("/me")
	public ResponseEntity<List<AbsenceResponse>> getMine() {
		return ResponseEntity.ok(absenceService.getMine());
	}

	@PostMapping
	public ResponseEntity<AbsenceResponse> create(@Valid @RequestBody AbsenceRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(absenceService.create(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<AbsenceResponse> update(@PathVariable Long id, @Valid @RequestBody AbsenceRequest request) {
		return ResponseEntity.ok(absenceService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		absenceService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{id}/justify")
	public ResponseEntity<AbsenceResponse> justify(@PathVariable Long id, @Valid @RequestBody JustifyAbsenceRequest request) {
		return ResponseEntity.ok(absenceService.justify(id, request));
	}

	@PutMapping("/{id}/validate")
	public ResponseEntity<AbsenceResponse> validate(@PathVariable Long id, @Valid @RequestBody ValidateAbsenceRequest request) {
		return ResponseEntity.ok(absenceService.validate(id, request));
	}
}
