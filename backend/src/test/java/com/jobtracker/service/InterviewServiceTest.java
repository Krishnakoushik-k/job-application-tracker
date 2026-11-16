package com.jobtracker.service;

import com.jobtracker.dto.InterviewRequest;
import com.jobtracker.dto.InterviewResponse;
import com.jobtracker.entity.Interview;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.enums.InterviewType;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.mapper.InterviewMapper;
import com.jobtracker.repository.InterviewRepository;
import com.jobtracker.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private JobApplicationRepository applicationRepository;

    private InterviewService service;

    @BeforeEach
    void setUp() {
        service = new InterviewService(interviewRepository, applicationRepository, new InterviewMapper(), FIXED_CLOCK);
    }

    @Test
    void createSchedulesInterviewForOwnedApplication() {
        JobApplication application = application(10L);
        when(applicationRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(application));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> inv.getArgument(0));

        InterviewResponse response = service.create(USER_ID, 10L, request());

        ArgumentCaptor<Interview> saved = ArgumentCaptor.forClass(Interview.class);
        verify(interviewRepository).save(saved.capture());
        assertThat(saved.getValue().getApplication()).isSameAs(application);
        assertThat(response.roundName()).isEqualTo("Technical Round");
        assertThat(response.companyName()).isEqualTo("Acme");
        assertThat(response.applicationId()).isEqualTo(10L);
    }

    @Test
    void createForSomeoneElsesApplicationIsNotFound() {
        when(applicationRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(OTHER_USER_ID, 10L, request()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).save(any(Interview.class));
    }

    @Test
    void updateAndDeleteOfSomeoneElsesInterviewAreNotFound() {
        when(interviewRepository.findByIdAndUser(99L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(OTHER_USER_ID, 99L, request()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(OTHER_USER_ID, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).save(any(Interview.class));
        verify(interviewRepository, never()).delete(any(Interview.class));
    }

    @Test
    void listForApplicationChecksOwnershipFirst() {
        when(applicationRepository.existsByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.listForApplication(OTHER_USER_ID, 10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).findByApplication(any(), any());
    }

    @Test
    void listUpcomingUsesClockAndClampsLimit() {
        when(interviewRepository.findUpcomingByUser(eq(USER_ID), eq(LocalDateTime.of(2026, 11, 10, 10, 0)),
                any(Pageable.class))).thenReturn(List.of());

        service.listUpcoming(USER_ID, 500);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(interviewRepository).findUpcomingByUser(eq(USER_ID), eq(LocalDateTime.of(2026, 11, 10, 10, 0)),
                pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
    }

    private static InterviewRequest request() {
        return new InterviewRequest("Technical Round", InterviewType.ONLINE,
                LocalDateTime.of(2026, 11, 20, 14, 30), "https://meet.example.com/x", null);
    }

    private static JobApplication application(Long id) {
        JobApplication application = new JobApplication();
        ReflectionTestUtils.setField(application, "id", id);
        application.setCompanyName("Acme");
        application.setJobTitle("Developer");
        return application;
    }
}
