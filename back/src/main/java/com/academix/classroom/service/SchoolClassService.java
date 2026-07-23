package com.academix.classroom.service;

import com.academix.auth.entity.Role;
import com.academix.auth.entity.User;
import com.academix.auth.repository.UserRepository;
import com.academix.classroom.dto.AssignTeacherRequest;
import com.academix.classroom.dto.ClassRequest;
import com.academix.classroom.dto.ClassResponse;
import com.academix.classroom.dto.MyClassAssignmentResponse;
import com.academix.classroom.dto.TeacherAssignmentResponse;
import com.academix.classroom.entity.Level;
import com.academix.classroom.entity.SchoolClass;
import com.academix.classroom.entity.TeacherAssignment;
import com.academix.classroom.repository.LevelRepository;
import com.academix.classroom.repository.SchoolClassRepository;
import com.academix.classroom.repository.TeacherAssignmentRepository;
import com.academix.student.dto.StudentResponse;
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
public class SchoolClassService {

	private final SchoolClassRepository schoolClassRepository;
	private final LevelRepository levelRepository;
	private final TeacherAssignmentRepository teacherAssignmentRepository;
	private final StudentRepository studentRepository;
	private final UserRepository userRepository;

	public SchoolClassService(SchoolClassRepository schoolClassRepository, LevelRepository levelRepository,
			TeacherAssignmentRepository teacherAssignmentRepository, StudentRepository studentRepository,
			UserRepository userRepository) {
		this.schoolClassRepository = schoolClassRepository;
		this.levelRepository = levelRepository;
		this.teacherAssignmentRepository = teacherAssignmentRepository;
		this.studentRepository = studentRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<ClassResponse> getAll() {
		return schoolClassRepository.findAllByOrderByLevelIdAscNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MyClassAssignmentResponse> getMine() {
		User teacher = currentTeacher();
		return teacherAssignmentRepository.findAllByTeacherId(teacher.getId()).stream()
				.map(assignment -> {
					SchoolClass schoolClass = assignment.getSchoolClass();
					return new MyClassAssignmentResponse(
							assignment.getId(),
							schoolClass.getId(),
							schoolClass.getName(),
							schoolClass.getLevel().getName(),
							assignment.getSubject(),
							(int) studentRepository.countBySchoolClassId(schoolClass.getId()));
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public List<StudentResponse> getRoster(Long classId) {
		SchoolClass schoolClass = findClass(classId);
		requireCanViewRoster(schoolClass);
		return studentRepository.findAllBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId).stream()
				.map(this::toStudentResponse)
				.toList();
	}

	public ClassResponse create(ClassRequest request) {
		Level level = findLevel(request.levelId());
		String name = request.name().trim();
		if (schoolClassRepository.existsByLevelIdAndNameIgnoreCase(level.getId(), name)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This level already has a class with that name");
		}
		return toResponse(schoolClassRepository.save(new SchoolClass(level, name)));
	}

	public ClassResponse update(Long id, ClassRequest request) {
		SchoolClass schoolClass = findClass(id);
		Level level = findLevel(request.levelId());
		String name = request.name().trim();
		if (!(schoolClass.getLevel().getId().equals(level.getId()) && schoolClass.getName().equalsIgnoreCase(name))
				&& schoolClassRepository.existsByLevelIdAndNameIgnoreCase(level.getId(), name)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This level already has a class with that name");
		}
		schoolClass.setLevel(level);
		schoolClass.setName(name);
		return toResponse(schoolClass);
	}

	public void delete(Long id) {
		SchoolClass schoolClass = findClass(id);
		studentRepository.findAllBySchoolClassIdOrderByLastNameAscFirstNameAsc(id)
				.forEach(student -> student.setSchoolClass(null));
		teacherAssignmentRepository.deleteAll(teacherAssignmentRepository.findAllBySchoolClassId(id));
		schoolClassRepository.delete(schoolClass);
	}

	public TeacherAssignmentResponse assignTeacher(Long classId, AssignTeacherRequest request) {
		SchoolClass schoolClass = findClass(classId);
		User teacher = userRepository.findByIdAndRole(request.teacherId(), Role.TEACHER)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
		String subject = request.subject().trim();
		if (teacherAssignmentRepository.existsByTeacherIdAndSchoolClassIdAndSubjectIgnoreCase(teacher.getId(), classId, subject)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This teacher is already assigned to this class for that subject");
		}
		TeacherAssignment assignment = teacherAssignmentRepository.save(new TeacherAssignment(teacher, schoolClass, subject));
		return toAssignmentResponse(assignment);
	}

	public void unassignTeacher(Long classId, Long assignmentId) {
		TeacherAssignment assignment = teacherAssignmentRepository.findById(assignmentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
		if (!assignment.getSchoolClass().getId().equals(classId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
		}
		teacherAssignmentRepository.delete(assignment);
	}

	private SchoolClass findClass(Long id) {
		return schoolClassRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
	}

	private Level findLevel(Long id) {
		return levelRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Level not found"));
	}

	private void requireCanViewRoster(SchoolClass schoolClass) {
		if (hasRole("ROLE_ADMINISTRATION")) {
			return;
		}
		if (hasRole("ROLE_TEACHER")) {
			User teacher = currentTeacher();
			if (teacherAssignmentRepository.existsByTeacherIdAndSchoolClassId(teacher.getId(), schoolClass.getId())) {
				return;
			}
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this class");
	}

	private User currentTeacher() {
		return userRepository.findByEmailIgnoreCase(currentUsername())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
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

	private ClassResponse toResponse(SchoolClass schoolClass) {
		List<TeacherAssignmentResponse> assignments = teacherAssignmentRepository.findAllBySchoolClassId(schoolClass.getId()).stream()
				.map(this::toAssignmentResponse)
				.toList();
		return new ClassResponse(
				schoolClass.getId(),
				schoolClass.getName(),
				schoolClass.getLevel().getId(),
				schoolClass.getLevel().getName(),
				(int) studentRepository.countBySchoolClassId(schoolClass.getId()),
				SchoolClass.CAPACITY,
				assignments);
	}

	private TeacherAssignmentResponse toAssignmentResponse(TeacherAssignment assignment) {
		User teacher = assignment.getTeacher();
		return new TeacherAssignmentResponse(assignment.getId(), teacher.getId(), teacher.getFullName(), assignment.getSubject());
	}

	private StudentResponse toStudentResponse(Student student) {
		SchoolClass schoolClass = student.getSchoolClass();
		return new StudentResponse(
				student.getId(),
				student.getUser() == null ? null : student.getUser().getId(),
				student.getFirstName(),
				student.getLastName(),
				student.getCin(),
				student.getStudentIdentifier(),
				student.getEmail(),
				student.getPhone(),
				student.getFieldOfStudy(),
				schoolClass == null ? null : schoolClass.getId(),
				schoolClass == null ? null : schoolClass.getName(),
				schoolClass == null ? null : schoolClass.getLevel().getName(),
				student.getPhotoUrl(),
				student.getAddress(),
				student.getCreatedAt(),
				student.getUpdatedAt());
	}
}
