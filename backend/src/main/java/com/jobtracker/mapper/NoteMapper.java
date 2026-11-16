package com.jobtracker.mapper;

import com.jobtracker.dto.NoteRequest;
import com.jobtracker.dto.NoteResponse;
import com.jobtracker.entity.ApplicationNote;
import com.jobtracker.entity.JobApplication;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

    public ApplicationNote toEntity(NoteRequest request, JobApplication application) {
        ApplicationNote note = new ApplicationNote();
        note.setApplication(application);
        applyRequest(note, request);
        return note;
    }

    public void updateEntity(ApplicationNote note, NoteRequest request) {
        applyRequest(note, request);
    }

    public NoteResponse toResponse(ApplicationNote note) {
        return new NoteResponse(
                note.getId(),
                note.getApplication().getId(),
                note.getCategory(),
                note.getContent(),
                note.getCreatedAt(),
                note.getUpdatedAt());
    }

    private void applyRequest(ApplicationNote note, NoteRequest request) {
        note.setCategory(request.category());
        note.setContent(request.content().trim());
    }
}
