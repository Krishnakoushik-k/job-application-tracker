package com.jobtracker.service;

import com.jobtracker.dto.NoteRequest;
import com.jobtracker.dto.NoteResponse;
import com.jobtracker.entity.ApplicationNote;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.mapper.NoteMapper;
import com.jobtracker.repository.ApplicationNoteRepository;
import com.jobtracker.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NoteService {

    private final ApplicationNoteRepository noteRepository;
    private final JobApplicationRepository applicationRepository;
    private final NoteMapper mapper;

    public NoteService(ApplicationNoteRepository noteRepository,
                       JobApplicationRepository applicationRepository,
                       NoteMapper mapper) {
        this.noteRepository = noteRepository;
        this.applicationRepository = applicationRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> listForApplication(Long userId, Long applicationId) {
        if (!applicationRepository.existsByIdAndUserId(applicationId, userId)) {
            throw new ResourceNotFoundException("Application not found");
        }
        return noteRepository.findByApplication(applicationId, userId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public NoteResponse create(Long userId, Long applicationId, NoteRequest request) {
        JobApplication application = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        ApplicationNote saved = noteRepository.save(mapper.toEntity(request, application));
        return mapper.toResponse(saved);
    }

    @Transactional
    public NoteResponse update(Long userId, Long noteId, NoteRequest request) {
        ApplicationNote note = findOwned(userId, noteId);
        mapper.updateEntity(note, request);
        return mapper.toResponse(noteRepository.save(note));
    }

    @Transactional
    public void delete(Long userId, Long noteId) {
        noteRepository.delete(findOwned(userId, noteId));
    }

    private ApplicationNote findOwned(Long userId, Long noteId) {
        return noteRepository.findByIdAndUser(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));
    }
}
