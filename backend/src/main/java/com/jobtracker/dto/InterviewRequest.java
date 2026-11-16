package com.jobtracker.dto;

import com.jobtracker.enums.InterviewType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record InterviewRequest(
        @Schema(example = "Technical Round 1")
        @NotBlank(message = "Round name is required")
        @Size(max = 100, message = "Round name must be at most 100 characters")
        String roundName,

        @Schema(example = "ONLINE")
        @NotNull(message = "Interview type is required")
        InterviewType interviewType,

        @Schema(example = "2026-11-20T14:30:00")
        @NotNull(message = "Interview date and time is required")
        LocalDateTime scheduledAt,

        @Schema(example = "https://meet.example.com/abc")
        @Size(max = 500, message = "Location or link must be at most 500 characters")
        String locationOrLink,

        @Size(max = 5000, message = "Notes must be at most 5000 characters")
        String notes
) {
}
