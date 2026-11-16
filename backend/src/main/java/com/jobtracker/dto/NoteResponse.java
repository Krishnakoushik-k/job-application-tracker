package com.jobtracker.dto;

import com.jobtracker.enums.NoteCategory;

import java.time.Instant;

public record NoteResponse(
        Long id,
        Long applicationId,
        NoteCategory category,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
}
