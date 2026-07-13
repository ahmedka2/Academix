package com.academix.config;

import com.academix.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/signup", "/api/auth/signin").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.GET, "/api/students", "/api/students/search").hasAnyRole("ADMINISTRATION", "TEACHER")
						.requestMatchers(HttpMethod.GET, "/api/students/me", "/api/students/*").hasAnyRole("ADMINISTRATION", "TEACHER", "STUDENT")
						.requestMatchers(HttpMethod.POST, "/api/students").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.PUT, "/api/students/*").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.DELETE, "/api/students/*").hasRole("ADMINISTRATION")
						.requestMatchers("/api/invoices/**").hasRole("ADMINISTRATION")
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}
