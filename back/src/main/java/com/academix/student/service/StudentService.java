package com.academix.student.service;

import com.academix.auth.entity.Role;
import com.academix.auth.entity.User;
import com.academix.auth.repository.UserRepository;
import com.academix.classroom.entity.SchoolClass;
import com.academix.classroom.repository.SchoolClassRepository;
import com.academix.student.dto.CreateStudentRequest;
import com.academix.student.dto.StudentRequest;
import com.academix.student.dto.StudentResponse;
import com.academix.student.dto.StudentSelfUpdateRequest;
import com.academix.student.entity.Student;
import com.academix.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class StudentService {

	private static final SecureRandom STUDENT_IDENTIFIER_RANDOM = new SecureRandom();
	private static final int STUDENT_IDENTIFIER_ATTEMPTS = 100;

	private final StudentRepository studentRepository;
	private final UserRepository userRepository;
	private final SchoolClassRepository schoolClassRepository;
	private final PasswordEncoder passwordEncoder;
	private final PhotoStorageService photoStorageService;

	public StudentService(StudentRepository studentRepository, UserRepository userRepository,
			SchoolClassRepository schoolClassRepository, PasswordEncoder passwordEncoder,
			PhotoStorageService photoStorageService) {
		this.studentRepository = studentRepository;
		this.userRepository = userRepository;
		this.schoolClassRepository = schoolClassRepository;
		this.passwordEncoder = passwordEncoder;
		this.photoStorageService = photoStorageService;
	}

	@Transactional(readOnly = true)
	public List<StudentResponse> getAllStudents() {
		requireAdministrationOrTeacher();
		return studentRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<StudentResponse> searchStudents(String query) {
		requireAdministrationOrTeacher();
		String normalizedQuery = query == null ? "" : query.trim();
		if (normalizedQuery.isBlank()) {
			return getAllStudents();
		}
		return studentRepository.search(normalizedQuery).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public StudentResponse getStudent(Long id) {
		Student student = findStudent(id);
		requireReadableStudent(student);
		return toResponse(student);
	}

	@Transactional(readOnly = true)
	public StudentResponse getCurrentStudent() {
		return toResponse(findCurrentStudent());
	}

	public StudentResponse updateOwnPhone(StudentSelfUpdateRequest request) {
		Student student = findCurrentStudent();
		student.setPhone(normalizeOptional(request.phone()));
		return toResponse(student);
	}

	public StudentResponse updateOwnPhoto(MultipartFile file) {
		Student student = findCurrentStudent();
		student.setPhotoUrl(photoStorageService.store(student.getId(), file));
		return toResponse(student);
	}

	public StudentResponse createStudent(CreateStudentRequest request) {
		requireAdministration();
		String email = normalizeEmail(request.email());
		String cin = normalizeRequired(request.cin());

		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "User email already exists");
		}
		if (studentRepository.existsByEmailIgnoreCase(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Student email already exists");
		}
		if (studentRepository.existsByCinIgnoreCase(cin)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Student CIN already exists");
		}

		User user = new User(
				fullName(request.firstName(), request.lastName()),
				email,
				passwordEncoder.encode(request.password()),
				Role.STUDENT);
		User savedUser = userRepository.save(user);

		Student student = new Student(
				savedUser,
				normalizeRequired(request.firstName()),
				normalizeRequired(request.lastName()),
				cin,
				"PENDING-" + UUID.randomUUID(),
				email,
				normalizeOptional(request.phone()),
				normalizeRequired(request.fieldOfStudy()),
				normalizeOptional(request.photoUrl()),
				normalizeOptional(request.address()));
		student.setSchoolClass(resolveClass(request.classId(), null));

		Student savedStudent = studentRepository.save(student);
		savedStudent.setStudentIdentifier(generateStudentIdentifier());
		return toResponse(savedStudent);
	}

	public StudentResponse updateStudent(Long id, StudentRequest request) {
		requireAdministration();
		Student student = findStudent(id);
		String email = normalizeEmail(request.email());
		String identifier = normalizeRequired(request.studentIdentifier());
		String cin = normalizeRequired(request.cin());

		studentRepository.findByEmailIgnoreCase(email)
				.filter(existing -> !existing.getId().equals(id))
				.ifPresent(existing -> {
					throw new ResponseStatusException(HttpStatus.CONFLICT, "Student email already exists");
				});
		userRepository.findByEmailIgnoreCase(email)
				.filter(existing -> student.getUser() == null || !existing.getId().equals(student.getUser().getId()))
				.ifPresent(existing -> {
					throw new ResponseStatusException(HttpStatus.CONFLICT, "User email already exists");
				});
		if (!student.getStudentIdentifier().equalsIgnoreCase(identifier)
				&& studentRepository.existsByStudentIdentifierIgnoreCase(identifier)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Student identifier already exists");
		}
		if (!student.getCin().equalsIgnoreCase(cin) && studentRepository.existsByCinIgnoreCase(cin)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Student CIN already exists");
		}

		student.setFirstName(normalizeRequired(request.firstName()));
		student.setLastName(normalizeRequired(request.lastName()));
		student.setCin(cin);
		student.setStudentIdentifier(identifier);
		student.setEmail(email);
		student.setPhone(normalizeOptional(request.phone()));
		student.setFieldOfStudy(normalizeRequired(request.fieldOfStudy()));
		student.setSchoolClass(resolveClass(request.classId(), id));
		student.setPhotoUrl(normalizeOptional(request.photoUrl()));
		student.setAddress(normalizeOptional(request.address()));
		if (student.getUser() != null) {
			student.getUser().setFullName(fullName(request.firstName(), request.lastName()));
			student.getUser().setEmail(email);
		}

		return toResponse(student);
	}

	public void deleteStudent(Long id) {
		requireAdministration();
		Student student = findStudent(id);
		User user = student.getUser();
		studentRepository.delete(student);
		if (user != null) {
			userRepository.delete(user);
		}
	}

	private Student findStudent(Long id) {
		return studentRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
	}

	private Student findCurrentStudent() {
		return studentRepository.findByEmailIgnoreCase(currentUsername())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
	}

	private SchoolClass resolveClass(Long classId, Long excludingStudentId) {
		if (classId == null) {
			return null;
		}
		SchoolClass schoolClass = schoolClassRepository.findById(classId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));

		long occupied = studentRepository.countBySchoolClassId(classId);
		boolean alreadyInThisClass = excludingStudentId != null && studentRepository.findById(excludingStudentId)
				.map(existing -> existing.getSchoolClass() != null && existing.getSchoolClass().getId().equals(classId))
				.orElse(false);
		if (!alreadyInThisClass && occupied >= SchoolClass.CAPACITY) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This class is full (" + SchoolClass.CAPACITY + " students)");
		}
		return schoolClass;
	}

	private StudentResponse toResponse(Student student) {
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

	private void requireReadableStudent(Student student) {
		if (hasRole("ROLE_ADMINISTRATION") || hasRole("ROLE_TEACHER")) {
			return;
		}
		if (hasRole("ROLE_STUDENT") && student.getEmail().equalsIgnoreCase(currentUsername())) {
			return;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this student profile");
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

	private String normalizeEmail(String email) {
		return normalizeRequired(email).toLowerCase();
	}

	private String generateStudentIdentifier() {
		for (int attempt = 0; attempt < STUDENT_IDENTIFIER_ATTEMPTS; attempt++) {
			String identifier = String.format(
					Locale.ROOT,
					"%03dacademix%03d",
					STUDENT_IDENTIFIER_RANDOM.nextInt(1_000),
					STUDENT_IDENTIFIER_RANDOM.nextInt(1_000));
			if (!studentRepository.existsByStudentIdentifierIgnoreCase(identifier)) {
				return identifier;
			}
		}
		throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not generate a unique student identifier");
	}

	private String fullName(String firstName, String lastName) {
		return normalizeRequired(firstName) + " " + normalizeRequired(lastName);
	}

	private String normalizeRequired(String value) {
		return value.trim();
	}

	private String normalizeOptional(String value) {
		if (value == null || value.trim().isBlank()) {
			return null;
		}
		return value.trim();
	}
}
