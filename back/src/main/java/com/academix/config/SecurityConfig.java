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
						.requestMatchers("/api/auth/signup", "/api/auth/signin", "/error").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.GET, "/api/students", "/api/students/search").hasAnyRole("ADMINISTRATION", "TEACHER")
						.requestMatchers(HttpMethod.GET, "/api/students/me", "/api/students/*").hasAnyRole("ADMINISTRATION", "TEACHER", "STUDENT")
						.requestMatchers(HttpMethod.POST, "/api/students").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.PUT, "/api/students/me").hasRole("STUDENT")
						.requestMatchers(HttpMethod.POST, "/api/students/me/photo").hasRole("STUDENT")
						.requestMatchers(HttpMethod.PUT, "/api/students/*").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.DELETE, "/api/students/*").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.GET, "/api/invoices/me").hasAnyRole("ADMINISTRATION", "TEACHER", "STUDENT")
						.requestMatchers("/api/invoices/**").hasRole("ADMINISTRATION")
						.requestMatchers("/api/teachers/**").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.GET, "/api/grades").hasAnyRole("ADMINISTRATION", "TEACHER")
						.requestMatchers(HttpMethod.GET, "/api/grades/me").hasAnyRole("ADMINISTRATION", "TEACHER", "STUDENT")
						.requestMatchers(HttpMethod.POST, "/api/grades/bulk").hasRole("TEACHER")
						.requestMatchers(HttpMethod.PUT, "/api/grades/*/validate").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.PUT, "/api/grades/*").hasAnyRole("ADMINISTRATION", "TEACHER")
						.requestMatchers(HttpMethod.DELETE, "/api/grades/*").hasAnyRole("ADMINISTRATION", "TEACHER")
						.requestMatchers(HttpMethod.GET, "/api/absences").hasAnyRole("ADMINISTRATION", "TEACHER")
						.requestMatchers(HttpMethod.GET, "/api/absences/me").hasAnyRole("ADMINISTRATION", "TEACHER", "STUDENT")
						.requestMatchers(HttpMethod.POST, "/api/absences/bulk").hasRole("TEACHER")
						.requestMatchers(HttpMethod.PUT, "/api/absences/*/justify").hasRole("STUDENT")
						.requestMatchers(HttpMethod.PUT, "/api/absences/*/validate").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.PUT, "/api/absences/*").hasAnyRole("ADMINISTRATION", "TEACHER")
						.requestMatchers(HttpMethod.DELETE, "/api/absences/*").hasAnyRole("ADMINISTRATION", "TEACHER")
						.requestMatchers("/api/statistics/**").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.GET, "/api/documents/me").hasAnyRole("ADMINISTRATION", "TEACHER", "STUDENT")
						.requestMatchers(HttpMethod.GET, "/api/documents/*/download").hasAnyRole("ADMINISTRATION", "STUDENT")
						.requestMatchers("/api/documents/**").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.POST, "/api/stage-documents/me/*").hasRole("STUDENT")
						.requestMatchers(HttpMethod.GET, "/api/stage-documents/me").hasAnyRole("ADMINISTRATION", "TEACHER", "STUDENT")
						.requestMatchers(HttpMethod.GET, "/api/stage-documents/*/download").hasAnyRole("ADMINISTRATION", "STUDENT")
						.requestMatchers(HttpMethod.PUT, "/api/stage-documents/*/validate").hasRole("ADMINISTRATION")
						.requestMatchers("/api/stage-documents/**").hasRole("ADMINISTRATION")
						.requestMatchers("/api/levels/**").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.GET, "/api/classes/mine").hasRole("TEACHER")
						.requestMatchers(HttpMethod.GET, "/api/classes/*/students").hasAnyRole("ADMINISTRATION", "TEACHER")
						.requestMatchers(HttpMethod.GET, "/api/classes", "/api/classes/*").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.POST, "/api/classes/*/assignments").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.DELETE, "/api/classes/*/assignments/*").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.POST, "/api/classes").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.PUT, "/api/classes/*").hasRole("ADMINISTRATION")
						.requestMatchers(HttpMethod.DELETE, "/api/classes/*").hasRole("ADMINISTRATION")
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}
