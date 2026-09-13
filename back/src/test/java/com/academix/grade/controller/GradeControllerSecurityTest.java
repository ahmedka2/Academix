package com.academix.grade.controller;

import com.academix.auth.repository.UserRepository;
import com.academix.config.SecurityConfig;
import com.academix.grade.dto.BulkGradeEntry;
import com.academix.grade.dto.BulkGradeRequest;
import com.academix.grade.entity.Semester;
import com.academix.grade.service.GradeService;
import com.academix.security.JwtAuthenticationFilter;
import com.academix.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web slice test: boots the MVC layer + the real SecurityConfig, with the service mocked.
 * Its job is to prove the SecurityConfig matcher list really enforces the documented
 * role rules - a check no service unit test can make, since the matcher list is the
 * first of the two enforcement points.
 */
@WebMvcTest(GradeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class GradeControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private GradeService gradeService;

	// Needed only so SecurityConfig's JwtAuthenticationFilter can be constructed.
	@MockitoBean
	private JwtService jwtService;
	@MockitoBean
	private UserRepository userRepository;

	private String bulkPayload() throws Exception {
		return objectMapper.writeValueAsString(new BulkGradeRequest(
				1L, "Math", new BigDecimal("2"), Semester.S1, "2024-2025",
				List.of(new BulkGradeEntry(1L, new BigDecimal("15")))));
	}

	@Test
	void anonymousRequestsAreRejected() throws Exception {
		// NOTE: 403, not 401. SecurityConfig declares no AuthenticationEntryPoint, so
		// Spring Security falls back to Http403ForbiddenEntryPoint for anonymous requests.
		// Asserting the real behaviour here; see the comment in SecurityConfig if this
		// is ever changed to a proper 401.
		mockMvc.perform(get("/api/grades"))
				.andExpect(status().isForbidden());
	}

	@Test
	void aStudentCannotListEveryGrade() throws Exception {
		mockMvc.perform(get("/api/grades").with(user("sarra@academix.com").roles("STUDENT")))
				.andExpect(status().isForbidden());
	}

	@Test
	void aTeacherCanListGrades() throws Exception {
		mockMvc.perform(get("/api/grades").with(user("teacher@academix.com").roles("TEACHER")))
				.andExpect(status().isOk());
	}

	@Test
	void onlyATeacherCanSubmitABulkRollCall() throws Exception {
		mockMvc.perform(post("/api/grades/bulk")
						.with(user("admin@academix.com").roles("ADMINISTRATION"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(bulkPayload()))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/grades/bulk")
						.with(user("teacher@academix.com").roles("TEACHER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(bulkPayload()))
				.andExpect(status().isCreated());
	}

	@Test
	void onlyAdministrationCanValidateAGrade() throws Exception {
		String payload = "{\"approved\":true,\"comment\":null}";

		mockMvc.perform(put("/api/grades/5/validate")
						.with(user("teacher@academix.com").roles("TEACHER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/grades/5/validate")
						.with(user("admin@academix.com").roles("ADMINISTRATION"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isOk());
	}

	@Test
	void theValidateMatcherIsNotShadowedByTheGenericPutMatcher() throws Exception {
		// /api/grades/*/validate is declared before /api/grades/* in SecurityConfig.
		// If that order is ever reversed, a TEACHER would be allowed through here.
		mockMvc.perform(put("/api/grades/5/validate")
						.with(user("teacher@academix.com").roles("TEACHER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"approved\":true}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void aBulkRequestWithAnInvalidAcademicYearIsA400NotA403() throws Exception {
		// Regression guard for the "/error must be permitAll" trap: without it, the
		// internal forward to /error turns every validation failure into a bare 403.
		String invalid = objectMapper.writeValueAsString(new BulkGradeRequest(
				1L, "Math", new BigDecimal("2"), Semester.S1, "not-a-year",
				List.of(new BulkGradeEntry(1L, new BigDecimal("15")))));

		mockMvc.perform(post("/api/grades/bulk")
						.with(user("teacher@academix.com").roles("TEACHER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest());
	}
}
