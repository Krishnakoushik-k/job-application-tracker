package com.jobtracker.controller;

import com.jobtracker.dto.InterviewRequest;
import com.jobtracker.dto.InterviewResponse;
import com.jobtracker.security.UserPrincipal;
import com.jobtracker.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Interviews", description = "Schedule and manage interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @Operation(summary = "List all my interviews (latest first)")
    @GetMapping("/interviews")
    public List<InterviewResponse> listAll(@AuthenticationPrincipal UserPrincipal principal) {
        return interviewService.listAll(principal.getId());
    }

    @Operation(summary = "List my upcoming interviews (soonest first)")
    @GetMapping("/interviews/upcoming")
    public List<InterviewResponse> listUpcoming(@AuthenticationPrincipal UserPrincipal principal,
                                                @RequestParam(defaultValue = "10") int limit) {
        return interviewService.listUpcoming(principal.getId(), limit);
    }

    @Operation(summary = "List the interviews of one application")
    @GetMapping("/applications/{applicationId}/interviews")
    public List<InterviewResponse> listForApplication(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable Long applicationId) {
        return interviewService.listForApplication(principal.getId(), applicationId);
    }

    @Operation(summary = "Schedule an interview for an application")
    @PostMapping("/applications/{applicationId}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long applicationId,
                                    @Valid @RequestBody InterviewRequest request) {
        return interviewService.create(principal.getId(), applicationId, request);
    }

    @Operation(summary = "Update an interview")
    @PutMapping("/interviews/{id}")
    public InterviewResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long id,
                                    @Valid @RequestBody InterviewRequest request) {
        return interviewService.update(principal.getId(), id, request);
    }

    @Operation(summary = "Delete an interview")
    @DeleteMapping("/interviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        interviewService.delete(principal.getId(), id);
    }
}
