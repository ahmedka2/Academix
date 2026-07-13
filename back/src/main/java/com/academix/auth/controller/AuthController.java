package com.academix.auth.controller;

import com.academix.auth.dto.AuthResponse;
import com.academix.auth.dto.SignInRequest;
import com.academix.auth.dto.SignUpRequest;
import com.academix.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/signup")
	public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
	}

	@PostMapping("/signin")
	public ResponseEntity<AuthResponse> signIn(@Valid @RequestBody SignInRequest request) {
		return ResponseEntity.ok(authService.signIn(request));
	}
}
