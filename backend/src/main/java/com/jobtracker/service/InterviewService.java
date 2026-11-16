package com.jobtracker.service;

import com.jobtracker.dto.InterviewRequest;
import com.jobtracker.dto.InterviewResponse;
import com.jobtracker.entity.Interview;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.mapper.InterviewMapper;
import com.jobtracker.repository.InterviewRepository;
import com.jobtracker.repository.JobApplicationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InterviewService {

    private static final int MAX_UPCOMING_LIMIT = 50;

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository applicationRepository;
    private final InterviewMapper mapper;
    private final Clock clock;

    public InterviewService(InterviewRepository interviewRepository,
                            JobApplicationRepository applicationRepository,
                            InterviewMapper mapper,
                            Clock clock) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> listForApplication(Long userId, Long applicationId) {
        ensureApplicationOwned(userId, applicationId);
        return interviewRepository.findByApplication(applicationId, userId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> listAll(Long userId) {
        return interviewRepository.findAllByUser(userId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> listUpcoming(Long userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_UPCOMING_LIMIT);
        return interviewRepository
                .findUpcomingByUser(userId, LocalDateTime.now(clock), PageRequest.of(0, safeLimit))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public InterviewResponse create(Long userId, Long applicationId, InterviewRequest request) {
        JobApplication application = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        Interview saved = interviewRepository.save(mapper.toEntity(request, application));
        return mapper.toResponse(saved);
    }

    @Transactional
    public InterviewResponse update(Long userId, Long interviewId, InterviewRequest request) {
        Interview interview = findOwned(userId, interviewId);
        mapper.updateEntity(interview, request);
        return mapper.toResponse(interviewRepository.save(interview));
    }

    @Transactional
    public void delete(Long userId, Long interviewId) {
        interviewRepository.delete(findOwned(userId, interviewId));
    }

    private Interview findOwned(Long userId, Long interviewId) {
        return interviewRepository.findByIdAndUser(interviewId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
    }

    private void ensureApplicationOwned(Long userId, Long applicationId) {
        if (!applicationRepository.existsByIdAndUserId(applicationId, userId)) {
            throw new ResourceNotFoundException("Application not found");
        }
    }
}
