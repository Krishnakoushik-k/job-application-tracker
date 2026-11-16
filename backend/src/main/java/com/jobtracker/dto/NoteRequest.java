package com.jobtracker.dto;

import com.jobtracker.enums.NoteCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @Schema(example = "PREPARATION")
        @NotNull(message = "Category is required")
        NoteCategory category,

        @Schema(example = "Revise Spring Security and JPA lazy loading.")
        @NotBlank(message = "Note content is required")
        @Size(max = 5000, message = "Note must be at most 5000 characters")
        String content
) {
}
