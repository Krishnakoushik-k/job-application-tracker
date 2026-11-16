package com.jobtracker.dto;

import com.jobtracker.enums.InterviewType;

import java.time.Instant;
import java.time.LocalDateTime;

public record InterviewResponse(
        Long id,
        Long applicationId,
        String companyName,
        String jobTitle,
        String roundName,
        InterviewType interviewType,
        LocalDateTime scheduledAt,
        String locationOrLink,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
