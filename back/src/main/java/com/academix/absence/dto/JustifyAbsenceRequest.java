package com.academix.absence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JustifyAbsenceRequest(
		@NotBlank(message = "Justification is required")
		@Size(max = 1000, message = "Justification must be at most 1000 characters") String justification) {
}
