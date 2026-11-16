package com.jobtracker.controller;

import com.jobtracker.dto.NoteRequest;
import com.jobtracker.dto.NoteResponse;
import com.jobtracker.security.UserPrincipal;
import com.jobtracker.service.NoteService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Application notes", description = "Recruiter conversations, feedback, reminders and preparation notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @Operation(summary = "List the notes of one application (newest first)")
    @GetMapping("/applications/{applicationId}/notes")
    public List<NoteResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable Long applicationId) {
        return noteService.listForApplication(principal.getId(), applicationId);
    }

    @Operation(summary = "Add a note to an application")
    @PostMapping("/applications/{applicationId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable Long applicationId,
                               @Valid @RequestBody NoteRequest request) {
        return noteService.create(principal.getId(), applicationId, request);
    }

    @Operation(summary = "Update a note")
    @PutMapping("/notes/{id}")
    public NoteResponse update(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable Long id,
                               @Valid @RequestBody NoteRequest request) {
        return noteService.update(principal.getId(), id, request);
    }

    @Operation(summary = "Delete a note")
    @DeleteMapping("/notes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        noteService.delete(principal.getId(), id);
    }
}
