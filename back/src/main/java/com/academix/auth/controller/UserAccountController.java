package com.academix.auth.controller;

import com.academix.auth.dto.CreateAccountRequest;
import com.academix.auth.dto.UserResponse;
import com.academix.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserAccountController {

	private final AuthService authService;

	public UserAccountController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping
	public ResponseEntity<UserResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.createAccount(request));
	}
}
