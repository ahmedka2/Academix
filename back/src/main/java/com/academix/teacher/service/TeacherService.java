package com.academix.teacher.service;

import com.academix.auth.entity.Role;
import com.academix.auth.entity.User;
import com.academix.auth.repository.UserRepository;
import com.academix.teacher.dto.CreateTeacherRequest;
import com.academix.teacher.dto.TeacherRequest;
import com.academix.teacher.dto.TeacherResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class TeacherService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public TeacherService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<TeacherResponse> getAllTeachers() {
		requireAdministration();
		return userRepository.findAllByRole(Role.TEACHER).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public TeacherResponse getTeacher(Long id) {
		requireAdministration();
		return toResponse(findTeacher(id));
	}

	public TeacherResponse createTeacher(CreateTeacherRequest request) {
		requireAdministration();
		String email = normalizeEmail(request.email());

		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "User email already exists");
		}

		User user = new User(request.fullName().trim(), email, passwordEncoder.encode(request.password()), Role.TEACHER);
		User savedUser = userRepository.save(user);

		return toResponse(savedUser);
	}

	public TeacherResponse updateTeacher(Long id, TeacherRequest request) {
		requireAdministration();
		User teacher = findTeacher(id);
		String email = normalizeEmail(request.email());

		userRepository.findByEmailIgnoreCase(email)
				.filter(existing -> !existing.getId().equals(id))
				.ifPresent(existing -> {
					throw new ResponseStatusException(HttpStatus.CONFLICT, "User email already exists");
				});

		teacher.setFullName(request.fullName().trim());
		teacher.setEmail(email);

		return toResponse(teacher);
	}

	public void deleteTeacher(Long id) {
		requireAdministration();
		userRepository.delete(findTeacher(id));
	}

	private User findTeacher(Long id) {
		return userRepository.findByIdAndRole(id, Role.TEACHER)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
	}

	private TeacherResponse toResponse(User user) {
		return new TeacherResponse(user.getId(), user.getFullName(), user.getEmail(), user.getCreatedAt(), user.getUpdatedAt());
	}

	private void requireAdministration() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRATION"));
		if (!isAdmin) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administration access required");
		}
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}
}
