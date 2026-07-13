package com.academix.auth.service;

import com.academix.auth.dto.AuthResponse;
import com.academix.auth.dto.CreateAccountRequest;
import com.academix.auth.dto.SignInRequest;
import com.academix.auth.dto.SignUpRequest;
import com.academix.auth.dto.UserResponse;
import com.academix.auth.entity.User;
import com.academix.auth.entity.Role;
import com.academix.auth.repository.UserRepository;
import com.academix.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public AuthResponse signUp(SignUpRequest request) {
		if (userRepository.count() > 0) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Public signup is disabled after the first admin account is created");
		}
		Role role = Role.fromCode(request.role());
		if (role != Role.ADMINISTRATION) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The first account must be an administration account");
		}

		String normalizedEmail = normalizeEmail(request.email());

		if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Cant't create account.");
		}

		User user = new User(request.fullName().trim(), normalizedEmail, passwordEncoder.encode(request.password()), role);
		User savedUser = userRepository.save(user);

		return toAuthResponse("Signup successful", savedUser);
	}

	public UserResponse createAccount(CreateAccountRequest request) {
		requireAdministration();
		String normalizedEmail = normalizeEmail(request.email());

		if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Cant't create account.");
		}

		Role role = Role.fromCode(request.role());
		User user = new User(request.fullName().trim(), normalizedEmail, passwordEncoder.encode(request.password()), role);
		User savedUser = userRepository.save(user);

		return toResponse(savedUser);
	}

	@Transactional(readOnly = true)
	public AuthResponse signIn(SignInRequest request) {
		String normalizedEmail = normalizeEmail(request.email());
		User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}

		return toAuthResponse("Signin successful", user);
	}

	private AuthResponse toAuthResponse(String message, User user) {
		return new AuthResponse(message, toResponse(user), jwtService.generateToken(user), "Bearer");
	}

	private UserResponse toResponse(User user) {
		Role role = user.getRole() == null ? Role.STUDENT : user.getRole();
		return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), role.getCode());
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
