package com.jobtracker.mapper;

import com.jobtracker.dto.ApplicationRequest;
import com.jobtracker.dto.ApplicationResponse;
import com.jobtracker.dto.ApplicationSummaryResponse;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {

    public JobApplication toEntity(ApplicationRequest request, User owner) {
        JobApplication application = new JobApplication();
        application.setUser(owner);
        applyRequest(application, request);
        return application;
    }

    /** Copies every editable field. The owner is never changed by an update. */
    public void updateEntity(JobApplication application, ApplicationRequest request) {
        applyRequest(application, request);
    }

    public ApplicationResponse toResponse(JobApplication a) {
        return new ApplicationResponse(
                a.getId(),
                a.getCompanyName(),
                a.getJobTitle(),
                a.getJobDescription(),
                a.getLocation(),
                a.getJobType(),
                a.getApplicationDate(),
                a.getStatus(),
                a.getJobUrl(),
                a.getSalaryRange(),
                a.getRecruiterName(),
                a.getRecruiterEmail(),
                a.getNotes(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }

    public ApplicationSummaryResponse toSummary(JobApplication a) {
        return new ApplicationSummaryResponse(
                a.getId(),
                a.getCompanyName(),
                a.getJobTitle(),
                a.getLocation(),
                a.getJobType(),
                a.getApplicationDate(),
                a.getStatus(),
                a.getSalaryRange(),
                a.getJobUrl());
    }

    private void applyRequest(JobApplication application, ApplicationRequest request) {
        application.setCompanyName(request.companyName().trim());
        application.setJobTitle(request.jobTitle().trim());
        application.setJobDescription(blankToNull(request.jobDescription()));
        application.setLocation(blankToNull(request.location()));
        application.setJobType(request.jobType());
        application.setApplicationDate(request.applicationDate());
        application.setStatus(request.status());
        application.setJobUrl(blankToNull(request.jobUrl()));
        application.setSalaryRange(blankToNull(request.salaryRange()));
        application.setRecruiterName(blankToNull(request.recruiterName()));
        application.setRecruiterEmail(blankToNull(request.recruiterEmail()));
        application.setNotes(blankToNull(request.notes()));
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
