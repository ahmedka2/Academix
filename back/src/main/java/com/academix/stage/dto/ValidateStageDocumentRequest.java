package com.academix.stage.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ValidateStageDocumentRequest(
		@NotNull(message = "Approved is required") Boolean approved,
		@Size(max = 500, message = "Comment must be at most 500 characters") String comment) {
}
