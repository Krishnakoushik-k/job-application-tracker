package com.jobtracker.dto;

import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.enums.JobType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Request body for creating and for updating (full replacement of) an application. */
public record ApplicationRequest(
        @Schema(example = "Globex Technologies")
        @NotBlank(message = "Company name is required")
        @Size(max = 150, message = "Company name must be at most 150 characters")
        String companyName,

        @Schema(example = "Full Stack Developer")
        @NotBlank(message = "Job title is required")
        @Size(max = 150, message = "Job title must be at most 150 characters")
        String jobTitle,

        @Size(max = 10000, message = "Job description must be at most 10000 characters")
        String jobDescription,

        @Schema(example = "Bengaluru, India")
        @Size(max = 150, message = "Location must be at most 150 characters")
        String location,

        @Schema(example = "FULL_TIME")
        @NotNull(message = "Job type is required")
        JobType jobType,

        @Schema(example = "2026-11-10")
        @NotNull(message = "Application date is required")
        LocalDate applicationDate,

        @Schema(example = "APPLIED")
        @NotNull(message = "Status is required")
        ApplicationStatus status,

        @Schema(example = "https://example.com/jobs/123")
        @Size(max = 500, message = "Job URL must be at most 500 characters")
        @Pattern(regexp = "^(https?://\\S+)?$", message = "Job URL must start with http:// or https://")
        String jobUrl,

        @Schema(example = "INR 10-14 LPA")
        @Size(max = 100, message = "Salary range must be at most 100 characters")
        String salaryRange,

        @Size(max = 100, message = "Recruiter name must be at most 100 characters")
        String recruiterName,

        @Email(message = "Recruiter email must be a valid email address")
        @Size(max = 255, message = "Recruiter email must be at most 255 characters")
        String recruiterEmail,

        @Size(max = 5000, message = "Notes must be at most 5000 characters")
        String notes
) {
}
