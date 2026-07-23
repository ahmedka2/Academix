package com.academix.absence.service;

import com.academix.absence.dto.AbsenceRequest;
import com.academix.absence.dto.AbsenceResponse;
import com.academix.absence.dto.JustifyAbsenceRequest;
import com.academix.absence.dto.ValidateAbsenceRequest;
import com.academix.absence.entity.Absence;
import com.academix.absence.entity.AbsenceStatus;
import com.academix.absence.repository.AbsenceRepository;
import com.academix.student.entity.Student;
import com.academix.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class AbsenceService {

	private final AbsenceRepository absenceRepository;
	private final StudentRepository studentRepository;

	public AbsenceService(AbsenceRepository absenceRepository, StudentRepository studentRepository) {
		this.absenceRepository = absenceRepository;
		this.studentRepository = studentRepository;
	}

	@Transactional(readOnly = true)
	public List<AbsenceResponse> getAll() {
		requireAdministrationOrTeacher();
		return absenceRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AbsenceResponse> getMine() {
		return absenceRepository.findByStudentId(currentStudent().getId()).stream()
				.map(this::toResponse)
				.toList();
	}

	public AbsenceResponse create(AbsenceRequest request) {
		requireAdministrationOrTeacher();
		Absence absence = new Absence(findStudent(request.studentId()), request.subject().trim(), request.date(), normalizeOptional(request.comment()));
		return toResponse(absenceRepository.save(absence));
	}

	public AbsenceResponse update(Long id, AbsenceRequest request) {
		requireAdministrationOrTeacher();
		Absence absence = findAbsence(id);
		absence.setStudent(findStudent(request.studentId()));
		absence.setSubject(request.subject().trim());
		absence.setDate(request.date());
		absence.setComment(normalizeOptional(request.comment()));
		return toResponse(absence);
	}

	public void delete(Long id) {
		requireAdministrationOrTeacher();
		absenceRepository.delete(findAbsence(id));
	}

	public AbsenceResponse justify(Long id, JustifyAbsenceRequest request) {
		Absence absence = findAbsence(id);
		Student student = currentStudent();
		if (!absence.getStudent().getId().equals(student.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot justify this absence");
		}
		if (absence.getStatus() != AbsenceStatus.UNJUSTIFIED && absence.getStatus() != AbsenceStatus.REJECTED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This absence is not awaiting justification");
		}
		absence.setJustification(request.justification().trim());
		absence.setStatus(AbsenceStatus.PENDING_JUSTIFICATION);
		return toResponse(absence);
	}

	public AbsenceResponse validate(Long id, ValidateAbsenceRequest request) {
		requireAdministration();
		Absence absence = findAbsence(id);
		if (absence.getStatus() != AbsenceStatus.PENDING_JUSTIFICATION) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This absence is not pending justification");
		}
		absence.setStatus(request.approved() ? AbsenceStatus.JUSTIFIED : AbsenceStatus.REJECTED);
		return toResponse(absence);
	}

	private Absence findAbsence(Long id) {
		return absenceRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Absence not found"));
	}

	private Student findStudent(Long id) {
		return studentRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
	}

	private Student currentStudent() {
		return studentRepository.findByEmailIgnoreCase(currentUsername())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
	}

	private AbsenceResponse toResponse(Absence absence) {
		Student student = absence.getStudent();
		return new AbsenceResponse(
				absence.getId(),
				student.getId(),
				student.getFirstName() + " " + student.getLastName(),
				student.getStudentIdentifier(),
				absence.getSubject(),
				absence.getDate(),
				absence.getComment(),
				absence.getJustification(),
				absence.getStatus(),
				absence.getCreatedAt(),
				absence.getUpdatedAt());
	}

	private void requireAdministrationOrTeacher() {
		if (!hasRole("ROLE_ADMINISTRATION") && !hasRole("ROLE_TEACHER")) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administration or teacher access required");
		}
	}

	private void requireAdministration() {
		if (!hasRole("ROLE_ADMINISTRATION")) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administration access required");
		}
	}

	private boolean hasRole(String role) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals(role));
	}

	private String currentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getName() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		return authentication.getName();
	}

	private String normalizeOptional(String value) {
		if (value == null || value.trim().isBlank()) {
			return null;
		}
		return value.trim();
	}
}
