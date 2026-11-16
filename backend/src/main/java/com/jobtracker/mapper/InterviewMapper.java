package com.jobtracker.mapper;

import com.jobtracker.dto.InterviewRequest;
import com.jobtracker.dto.InterviewResponse;
import com.jobtracker.entity.Interview;
import com.jobtracker.entity.JobApplication;
import org.springframework.stereotype.Component;

@Component
public class InterviewMapper {

    public Interview toEntity(InterviewRequest request, JobApplication application) {
        Interview interview = new Interview();
        interview.setApplication(application);
        applyRequest(interview, request);
        return interview;
    }

    public void updateEntity(Interview interview, InterviewRequest request) {
        applyRequest(interview, request);
    }

    /** Must be called inside a transaction (or with the application already fetched). */
    public InterviewResponse toResponse(Interview interview) {
        JobApplication application = interview.getApplication();
        return new InterviewResponse(
                interview.getId(),
                application.getId(),
                application.getCompanyName(),
                application.getJobTitle(),
                interview.getRoundName(),
                interview.getInterviewType(),
                interview.getScheduledAt(),
                interview.getLocationOrLink(),
                interview.getNotes(),
                interview.getCreatedAt(),
                interview.getUpdatedAt());
    }

    private void applyRequest(Interview interview, InterviewRequest request) {
        interview.setRoundName(request.roundName().trim());
        interview.setInterviewType(request.interviewType());
        interview.setScheduledAt(request.scheduledAt());
        interview.setLocationOrLink(ApplicationMapper.blankToNull(request.locationOrLink()));
        interview.setNotes(ApplicationMapper.blankToNull(request.notes()));
    }
}
