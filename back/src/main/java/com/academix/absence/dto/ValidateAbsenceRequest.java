package com.academix.absence.dto;

import jakarta.validation.constraints.NotNull;

public record ValidateAbsenceRequest(
		@NotNull(message = "Approved is required") Boolean approved) {
}
