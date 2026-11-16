package com.jobtracker.dto;

import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.enums.JobType;

import java.time.LocalDate;

/** Compact row used by the paginated list and the dashboard (no long text fields). */
public record ApplicationSummaryResponse(
        Long id,
        String companyName,
        String jobTitle,
        String location,
        JobType jobType,
        LocalDate applicationDate,
        ApplicationStatus status,
        String salaryRange,
        String jobUrl
) {
}
