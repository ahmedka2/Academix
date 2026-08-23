package com.academix.grade.service;

import com.academix.auth.entity.User;
import com.academix.auth.repository.UserRepository;
import com.academix.classroom.repository.TeacherAssignmentRepository;
import com.academix.grade.dto.BulkGradeEntry;
import com.academix.grade.dto.BulkGradeRequest;
import com.academix.grade.dto.GradeRequest;
import com.academix.grade.dto.GradeResponse;
import com.academix.grade.dto.ValidateGradeRequest;
import com.academix.grade.entity.Grade;
import com.academix.grade.entity.GradeStatus;
import com.academix.grade.repository.GradeRepository;
import com.academix.student.entity.Student;
import com.academix.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class GradeService {

	private final GradeRepository gradeRepository;
	private final StudentRepository studentRepository;
	private final TeacherAssignmentRepository teacherAssignmentRepository;
	private final UserRepository userRepository;

	public GradeService(GradeRepository gradeRepository, StudentRepository studentRepository,
			TeacherAssignmentRepository teacherAssignmentRepository, UserRepository userRepository) {
		this.gradeRepository = gradeRepository;
		this.studentRepository = studentRepository;
		this.teacherAssignmentRepository = teacherAssignmentRepository;
		this.userRepository = userRepository;
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
				.filter(grade -> grade.getStatus() == GradeStatus.APPROVED)
				.map(this::toResponse)
				.toList();
	}

	public List<GradeResponse> createBulk(BulkGradeRequest request) {
		User teacher = requireAssignedTeacher(request.classId(), request.subject());
		String subject = request.subject().trim();
		List<Grade> created = request.entries().stream()
				.filter(entry -> entry.score() != null)
				.map(entry -> {
					Grade grade = new Grade(
							findStudent(entry.studentId()),
							subject,
							entry.score(),
							request.coefficient(),
							request.semester(),
							request.academicYear());
					grade.setSubmittedBy(teacher.getEmail());
					return gradeRepository.save(grade);
				})
				.toList();
		return created.stream().map(this::toResponse).toList();
	}

	public GradeResponse validate(Long id, ValidateGradeRequest request) {
		requireAdministration();
		Grade grade = findGrade(id);
		if (grade.getStatus() != GradeStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This grade is not pending review");
		}
		grade.setStatus(request.approved() ? GradeStatus.APPROVED : GradeStatus.REJECTED);
		grade.setReviewComment(normalizeOptional(request.comment()));
		grade.setReviewedAt(Instant.now());
		grade.setReviewedBy(currentUsername());
		return toResponse(grade);
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
		if (grade.getStatus() != GradeStatus.PENDING) {
			grade.setStatus(GradeStatus.PENDING);
			grade.setReviewComment(null);
			grade.setReviewedAt(null);
			grade.setReviewedBy(null);
		}
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

	private User requireAssignedTeacher(Long classId, String subject) {
		User teacher = userRepository.findByEmailIgnoreCase(currentUsername())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
		if (!teacherAssignmentRepository.existsByTeacherIdAndSchoolClassIdAndSubjectIgnoreCase(teacher.getId(), classId, subject.trim())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this class for that subject");
		}
		return teacher;
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
				grade.getAcademicYear(),
				grade.getStatus(),
				grade.getSubmittedBy(),
				grade.getReviewComment(),
				grade.getReviewedAt(),
				grade.getReviewedBy());
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
