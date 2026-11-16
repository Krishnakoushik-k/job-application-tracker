package com.jobtracker.service;

import com.jobtracker.dto.ApplicationRequest;
import com.jobtracker.dto.ApplicationResponse;
import com.jobtracker.dto.ApplicationSearchCriteria;
import com.jobtracker.dto.ApplicationSummaryResponse;
import com.jobtracker.dto.PageResponse;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.User;
import com.jobtracker.exception.BadRequestException;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.mapper.ApplicationMapper;
import com.jobtracker.repository.JobApplicationRepository;
import com.jobtracker.repository.JobApplicationSpecifications;
import com.jobtracker.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobApplicationService {

    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 10;

    private final JobApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper mapper;
    private final CsvExportService csvExportService;

    public JobApplicationService(JobApplicationRepository applicationRepository,
                                 UserRepository userRepository,
                                 ApplicationMapper mapper,
                                 CsvExportService csvExportService) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.csvExportService = csvExportService;
    }

    /**
     * Filtering, sorting and pagination all happen in SQL. sort is "newest" (default) or "oldest".
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationSummaryResponse> search(Long userId,
                                                           ApplicationSearchCriteria criteria,
                                                           String sort,
                                                           int page,
                                                           int size) {
        if (criteria.dateFrom() != null && criteria.dateTo() != null
                && criteria.dateFrom().isAfter(criteria.dateTo())) {
            throw new BadRequestException("dateFrom must not be after dateTo");
        }

        Specification<JobApplication> specification = Specification
                .where(JobApplicationSpecifications.ownedBy(userId))
                .and(JobApplicationSpecifications.matchesSearch(criteria.search()))
                .and(JobApplicationSpecifications.hasStatus(criteria.status()))
                .and(JobApplicationSpecifications.hasJobType(criteria.jobType()))
                .and(JobApplicationSpecifications.appliedOnOrAfter(criteria.dateFrom()))
                .and(JobApplicationSpecifications.appliedOnOrBefore(criteria.dateTo()));

        Page<JobApplication> result = applicationRepository.findAll(specification, buildPageable(sort, page, size));
        return PageResponse.from(result.map(mapper::toSummary));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(Long userId, Long applicationId) {
        return mapper.toResponse(findOwned(userId, applicationId));
    }

    @Transactional
    public ApplicationResponse create(Long userId, ApplicationRequest request) {
        // The owner always comes from the authenticated principal, never from the request body.
        User owner = userRepository.getReferenceById(userId);
        JobApplication saved = applicationRepository.save(mapper.toEntity(request, owner));
        return mapper.toResponse(saved);
    }

    @Transactional
    public ApplicationResponse update(Long userId, Long applicationId, ApplicationRequest request) {
        JobApplication application = findOwned(userId, applicationId);
        mapper.updateEntity(application, request);
        return mapper.toResponse(applicationRepository.save(application));
    }

    @Transactional
    public void delete(Long userId, Long applicationId) {
        applicationRepository.delete(findOwned(userId, applicationId));
    }

    /** CSV containing only the authenticated user's applications. */
    @Transactional(readOnly = true)
    public String exportCsv(Long userId) {
        List<JobApplication> applications =
                applicationRepository.findByUserIdOrderByApplicationDateDescIdDesc(userId);
        return csvExportService.toCsv(applications);
    }

    private JobApplication findOwned(Long userId, Long applicationId) {
        return applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private Pageable buildPageable(String sort, int page, int size) {
        Sort.Direction direction;
        if (sort == null || sort.isBlank() || "newest".equalsIgnoreCase(sort)) {
            direction = Sort.Direction.DESC;
        } else if ("oldest".equalsIgnoreCase(sort)) {
            direction = Sort.Direction.ASC;
        } else {
            throw new BadRequestException("sort must be 'newest' or 'oldest'");
        }
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        // id as tie-breaker keeps pagination stable when several applications share a date.
        return PageRequest.of(safePage, safeSize, Sort.by(direction, "applicationDate", "id"));
    }
}
