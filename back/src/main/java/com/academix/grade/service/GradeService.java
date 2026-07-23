package com.academix.grade.service;

import com.academix.grade.dto.GradeRequest;
import com.academix.grade.dto.GradeResponse;
import com.academix.grade.entity.Grade;
import com.academix.grade.repository.GradeRepository;
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
public class GradeService {

	private final GradeRepository gradeRepository;
	private final StudentRepository studentRepository;

	public GradeService(GradeRepository gradeRepository, StudentRepository studentRepository) {
		this.gradeRepository = gradeRepository;
		this.studentRepository = studentRepository;
	}

	@Transactional(readOnly = true)
	public List<GradeResponse> getAll() {
		requireAdministrationOrTeacher();
		return gradeRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<GradeResponse> getMine() {
		String email = currentUsername();
		Student student = studentRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
		return gradeRepository.findByStudentId(student.getId()).stream()
				.map(this::toResponse)
				.toList();
	}

	public GradeResponse create(GradeRequest request) {
		requireAdministrationOrTeacher();
		Grade grade = new Grade(
				findStudent(request.studentId()),
				request.subject().trim(),
				request.score(),
				request.coefficient(),
				request.semester(),
				request.academicYear());
		return toResponse(gradeRepository.save(grade));
	}

	public GradeResponse update(Long id, GradeRequest request) {
		requireAdministrationOrTeacher();
		Grade grade = findGrade(id);
		grade.setStudent(findStudent(request.studentId()));
		grade.setSubject(request.subject().trim());
		grade.setScore(request.score());
		grade.setCoefficient(request.coefficient());
		grade.setSemester(request.semester());
		grade.setAcademicYear(request.academicYear());
		return toResponse(grade);
	}

	public void delete(Long id) {
		requireAdministrationOrTeacher();
		gradeRepository.delete(findGrade(id));
	}

	private Grade findGrade(Long id) {
		return gradeRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade not found"));
	}

	private Student findStudent(Long id) {
		return studentRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
	}

	private GradeResponse toResponse(Grade grade) {
		Student student = grade.getStudent();
		return new GradeResponse(
				grade.getId(),
				student.getId(),
				student.getFirstName() + " " + student.getLastName(),
				student.getStudentIdentifier(),
				grade.getSubject(),
				grade.getScore(),
				grade.getCoefficient(),
				grade.getSemester(),
				grade.getAcademicYear());
	}

	private void requireAdministrationOrTeacher() {
		if (!hasRole("ROLE_ADMINISTRATION") && !hasRole("ROLE_TEACHER")) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administration or teacher access required");
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
}
