package com.academix.grade.service;

import com.academix.auth.entity.Role;
import com.academix.auth.entity.User;
import com.academix.auth.repository.UserRepository;
import com.academix.classroom.repository.TeacherAssignmentRepository;
import com.academix.grade.dto.BulkGradeEntry;
import com.academix.grade.dto.BulkGradeRequest;
import com.academix.grade.dto.GradeResponse;
import com.academix.grade.dto.ValidateGradeRequest;
import com.academix.grade.entity.Grade;
import com.academix.grade.entity.GradeStatus;
import com.academix.grade.entity.Semester;
import com.academix.grade.repository.GradeRepository;
import com.academix.student.entity.Student;
import com.academix.student.repository.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test of the grade business rules, with every repository mocked.
 * This is the layer worth testing most: all the role checks, the 409s and the
 * PENDING -> APPROVED workflow live in the service, not in the controller.
 */
@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

	@Mock
	private GradeRepository gradeRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private TeacherAssignmentRepository teacherAssignmentRepository;
	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private GradeService gradeService;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	private void authenticateAs(String email, String role) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(email, null,
						List.of(new SimpleGrantedAuthority("ROLE_" + role))));
	}

	private Student student(long id, String firstName) {
		Student student = new Student(null, firstName, "Trabelsi", "0123456" + id,
				"100academix00" + id, firstName + "@academix.com", null, "Informatique", null, null);
		student.setId(id);
		return student;
	}

	private Grade pendingGrade() {
		return new Grade(student(1L, "Sarra"), "Math", new BigDecimal("14.5"),
				new BigDecimal("2"), Semester.S1, "2024-2025");
	}

	// --- createBulk -------------------------------------------------------

	@Test
	void createBulkSkipsEntriesWithoutAScore() {
		authenticateAs("teacher@academix.com", "TEACHER");
		User teacher = new User("Amel", "teacher@academix.com", "hash", Role.TEACHER);
		teacher.setId(7L);
		when(userRepository.findByEmailIgnoreCase("teacher@academix.com")).thenReturn(Optional.of(teacher));
		when(teacherAssignmentRepository
				.existsByTeacherIdAndSchoolClassIdAndSubjectIgnoreCase(7L, 3L, "Math")).thenReturn(true);
		when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L, "Sarra")));
		when(gradeRepository.save(any(Grade.class))).thenAnswer(call -> call.getArgument(0));

		List<GradeResponse> created = gradeService.createBulk(new BulkGradeRequest(
				3L, "Math", new BigDecimal("2"), Semester.S1, "2024-2025",
				List.of(new BulkGradeEntry(1L, new BigDecimal("14.5")),
						new BulkGradeEntry(2L, null))));

		// The absent student (null score) produces no row at all.
		assertThat(created).hasSize(1);
		verify(gradeRepository, never()).save(null);
		verify(studentRepository, never()).findById(2L);
	}

	@Test
	void createBulkStartsEveryGradeAsPending() {
		authenticateAs("teacher@academix.com", "TEACHER");
		User teacher = new User("Amel", "teacher@academix.com", "hash", Role.TEACHER);
		teacher.setId(7L);
		when(userRepository.findByEmailIgnoreCase("teacher@academix.com")).thenReturn(Optional.of(teacher));
		when(teacherAssignmentRepository
				.existsByTeacherIdAndSchoolClassIdAndSubjectIgnoreCase(7L, 3L, "Math")).thenReturn(true);
		when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L, "Sarra")));
		when(gradeRepository.save(any(Grade.class))).thenAnswer(call -> call.getArgument(0));

		List<GradeResponse> created = gradeService.createBulk(new BulkGradeRequest(
				3L, "Math", new BigDecimal("2"), Semester.S1, "2024-2025",
				List.of(new BulkGradeEntry(1L, new BigDecimal("14.5")))));

		assertThat(created).singleElement()
				.satisfies(grade -> {
					assertThat(grade.status()).isEqualTo(GradeStatus.PENDING);
					assertThat(grade.submittedBy()).isEqualTo("teacher@academix.com");
				});
	}

	@Test
	void createBulkIsForbiddenForATeacherNotAssignedToThatClassAndSubject() {
		authenticateAs("teacher@academix.com", "TEACHER");
		User teacher = new User("Amel", "teacher@academix.com", "hash", Role.TEACHER);
		teacher.setId(7L);
		when(userRepository.findByEmailIgnoreCase("teacher@academix.com")).thenReturn(Optional.of(teacher));
		when(teacherAssignmentRepository
				.existsByTeacherIdAndSchoolClassIdAndSubjectIgnoreCase(7L, 99L, "Physique")).thenReturn(false);

		BulkGradeRequest request = new BulkGradeRequest(99L, "Physique", new BigDecimal("2"),
				Semester.S1, "2024-2025", List.of(new BulkGradeEntry(1L, new BigDecimal("12"))));

		assertThatThrownBy(() -> gradeService.createBulk(request))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(error -> ((ResponseStatusException) error).getStatusCode())
				.isEqualTo(HttpStatus.FORBIDDEN);
		verify(gradeRepository, never()).save(any());
	}

	// --- validate ---------------------------------------------------------

	@Test
	void adminApprovalMovesAPendingGradeToApproved() {
		authenticateAs("admin@academix.com", "ADMINISTRATION");
		Grade grade = pendingGrade();
		when(gradeRepository.findById(5L)).thenReturn(Optional.of(grade));

		GradeResponse response = gradeService.validate(5L, new ValidateGradeRequest(true, null));

		assertThat(response.status()).isEqualTo(GradeStatus.APPROVED);
		assertThat(grade.getReviewedBy()).isEqualTo("admin@academix.com");
		assertThat(grade.getReviewedAt()).isNotNull();
	}

	@Test
	void validatingAnAlreadyReviewedGradeIsAConflict() {
		authenticateAs("admin@academix.com", "ADMINISTRATION");
		Grade grade = pendingGrade();
		grade.setStatus(GradeStatus.APPROVED);
		when(gradeRepository.findById(5L)).thenReturn(Optional.of(grade));

		ValidateGradeRequest request = new ValidateGradeRequest(true, null);

		assertThatThrownBy(() -> gradeService.validate(5L, request))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(error -> ((ResponseStatusException) error).getStatusCode())
				.isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void aTeacherCannotValidateAGrade() {
		authenticateAs("teacher@academix.com", "TEACHER");

		ValidateGradeRequest request = new ValidateGradeRequest(true, null);

		assertThatThrownBy(() -> gradeService.validate(5L, request))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(error -> ((ResponseStatusException) error).getStatusCode())
				.isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void missingGradeIsReportedAsNotFound() {
		authenticateAs("admin@academix.com", "ADMINISTRATION");
		when(gradeRepository.findById(404L)).thenReturn(Optional.empty());

		ValidateGradeRequest request = new ValidateGradeRequest(true, null);

		assertThatThrownBy(() -> gradeService.validate(404L, request))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(error -> ((ResponseStatusException) error).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	// --- getMine ----------------------------------------------------------

	@Test
	void aStudentOnlySeesApprovedGrades() {
		authenticateAs("sarra@academix.com", "STUDENT");
		Student sarra = student(1L, "Sarra");
		when(studentRepository.findByEmailIgnoreCase("sarra@academix.com")).thenReturn(Optional.of(sarra));

		Grade approved = pendingGrade();
		approved.setStatus(GradeStatus.APPROVED);
		Grade pending = pendingGrade();
		Grade rejected = pendingGrade();
		rejected.setStatus(GradeStatus.REJECTED);
		when(gradeRepository.findByStudentId(1L)).thenReturn(List.of(approved, pending, rejected));

		List<GradeResponse> mine = gradeService.getMine();

		assertThat(mine).hasSize(1);
		assertThat(mine.get(0).status()).isEqualTo(GradeStatus.APPROVED);
	}
}
