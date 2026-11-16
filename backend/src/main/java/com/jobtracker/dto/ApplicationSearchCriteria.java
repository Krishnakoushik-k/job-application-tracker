package com.jobtracker.dto;

import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.enums.JobType;

import java.time.LocalDate;

/** Optional filters for the application list. Any null/blank field is ignored. */
public record ApplicationSearchCriteria(
        String search,
        ApplicationStatus status,
        JobType jobType,
        LocalDate dateFrom,
        LocalDate dateTo
) {
}
