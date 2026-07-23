package com.academix.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClassRequest(
		@NotNull(message = "Level is required") Long levelId,
		@NotBlank(message = "Name is required")
		@Size(max = 100, message = "Name must be at most 100 characters") String name) {
}
