package com.jobtracker.dto;

import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.enums.JobType;

import java.time.Instant;
import java.time.LocalDate;

/** Full detail view of one application. */
public record ApplicationResponse(
        Long id,
        String companyName,
        String jobTitle,
        String jobDescription,
        String location,
        JobType jobType,
        LocalDate applicationDate,
        ApplicationStatus status,
        String jobUrl,
        String salaryRange,
        String recruiterName,
        String recruiterEmail,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
