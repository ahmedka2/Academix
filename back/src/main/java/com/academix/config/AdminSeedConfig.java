package com.academix.config;

import com.academix.auth.entity.Role;
import com.academix.auth.entity.User;
import com.academix.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeedConfig {

	@Bean
	public CommandLineRunner seedAdminAccount(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.seed.admin.full-name}") String fullName,
			@Value("${app.seed.admin.email}") String email,
			@Value("${app.seed.admin.password}") String password) {
		return args -> {
			if (userRepository.count() > 0) {
				return;
			}

			User admin = new User(
					fullName.trim(),
					email.trim().toLowerCase(),
					passwordEncoder.encode(password),
					Role.ADMINISTRATION);
			userRepository.save(admin);
		};
	}
}
