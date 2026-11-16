package com.jobtracker.controller;

import com.jobtracker.dto.ApplicationRequest;
import com.jobtracker.dto.ApplicationResponse;
import com.jobtracker.dto.ApplicationSearchCriteria;
import com.jobtracker.dto.ApplicationSummaryResponse;
import com.jobtracker.dto.PageResponse;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.enums.JobType;
import com.jobtracker.security.UserPrincipal;
import com.jobtracker.service.JobApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Job applications", description = "CRUD, search, filtering, pagination and CSV export")
public class JobApplicationController {

    private final JobApplicationService applicationService;

    public JobApplicationController(JobApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "List my applications",
            description = "Filters are optional and combinable. sort is 'newest' (default) or 'oldest'. "
                    + "page is 0-based; size defaults to 10 and is capped at 100.")
    @GetMapping
    public PageResponse<ApplicationSummaryResponse> search(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) JobType jobType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ApplicationSearchCriteria criteria =
                new ApplicationSearchCriteria(search, status, jobType, dateFrom, dateTo);
        return applicationService.search(principal.getId(), criteria, sort, page, size);
    }

    @Operation(summary = "Export all my applications as CSV")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(@AuthenticationPrincipal UserPrincipal principal) {
        byte[] body = applicationService.exportCsv(principal.getId()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("job-applications.csv").build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @Operation(summary = "Get one application")
    @GetMapping("/{id}")
    public ApplicationResponse get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return applicationService.get(principal.getId(), id);
    }

    @Operation(summary = "Create an application")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                      @Valid @RequestBody ApplicationRequest request) {
        return applicationService.create(principal.getId(), request);
    }

    @Operation(summary = "Update an application (full replacement of editable fields)")
    @PutMapping("/{id}")
    public ApplicationResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long id,
                                      @Valid @RequestBody ApplicationRequest request) {
        return applicationService.update(principal.getId(), id, request);
    }

    @Operation(summary = "Delete an application together with its interviews and notes")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        applicationService.delete(principal.getId(), id);
    }
}
