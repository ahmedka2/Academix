package com.academix.security;

import com.academix.auth.entity.Role;
import com.academix.auth.entity.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plain unit test: no Spring context at all, so it runs in milliseconds.
 * JwtService is a good first target because the whole auth model depends on it.
 */
class JwtServiceTest {

	private static final String SECRET = "test-secret-test-secret-test-secret-test-secret-test-secret";

	private User teacher() {
		User user = new User("Amel Ben Salah", "teacher@academix.com", "hash", Role.TEACHER);
		user.setId(42L);
		return user;
	}

	@Test
	void generatedTokenCarriesTheUserEmailAsSubject() {
		JwtService jwtService = new JwtService(SECRET, 3_600_000L);

		String token = jwtService.generateToken(teacher());

		assertThat(jwtService.extractUsername(token)).isEqualTo("teacher@academix.com");
	}

	@Test
	void tokenIsValidForItsOwnUser() {
		JwtService jwtService = new JwtService(SECRET, 3_600_000L);

		String token = jwtService.generateToken(teacher());

		assertThat(jwtService.isTokenValid(token, "teacher@academix.com")).isTrue();
		assertThat(jwtService.isTokenValid(token, "someone.else@academix.com")).isFalse();
	}

	@Test
	void expiredTokenIsRejected() {
		// A negative expiry produces a token that was already expired when it was issued.
		JwtService jwtService = new JwtService(SECRET, -1_000L);

		String token = jwtService.generateToken(teacher());

		assertThatThrownBy(() -> jwtService.isTokenValid(token, "teacher@academix.com"))
				.isInstanceOf(JwtException.class);
	}

	@Test
	void tokenSignedWithAnotherSecretIsRejected() {
		JwtService issuer = new JwtService(SECRET, 3_600_000L);
		JwtService verifier = new JwtService("another-secret-another-secret-another-secret-xx", 3_600_000L);

		String token = issuer.generateToken(teacher());

		assertThatThrownBy(() -> verifier.extractUsername(token))
				.isInstanceOf(JwtException.class);
	}
}
