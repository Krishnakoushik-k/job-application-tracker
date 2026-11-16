package com.jobtracker.service;

import com.jobtracker.dto.ApplicationRequest;
import com.jobtracker.dto.ApplicationResponse;
import com.jobtracker.dto.ApplicationSearchCriteria;
import com.jobtracker.dto.ApplicationSummaryResponse;
import com.jobtracker.dto.PageResponse;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.User;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.enums.JobType;
import com.jobtracker.exception.BadRequestException;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.mapper.ApplicationMapper;
import com.jobtracker.repository.JobApplicationRepository;
import com.jobtracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long APPLICATION_ID = 5L;

    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private UserRepository userRepository;

    private JobApplicationService service;

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(applicationRepository, userRepository,
                new ApplicationMapper(), new CsvExportService());
    }

    @Test
    void getReturnsOwnedApplication() {
        when(applicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.of(application("Acme")));

        ApplicationResponse response = service.get(USER_ID, APPLICATION_ID);

        assertThat(response.companyName()).isEqualTo("Acme");
        assertThat(response.id()).isEqualTo(APPLICATION_ID);
    }

    @Test
    void getThrowsNotFoundWhenApplicationBelongsToSomeoneElse() {
        when(applicationRepository.findByIdAndUserId(APPLICATION_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(OTHER_USER_ID, APPLICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createAssignsAuthenticatedUserAsOwnerAndCleansInput() {
        User owner = new User();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationRequest request = new ApplicationRequest("  Acme  ", " Developer ", null, "   ",
                JobType.FULL_TIME, LocalDate.of(2026, 11, 1), ApplicationStatus.APPLIED,
                null, null, null, "", "   ");

        ApplicationResponse response = service.create(USER_ID, request);

        ArgumentCaptor<JobApplication> saved = ArgumentCaptor.forClass(JobApplication.class);
        verify(applicationRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(owner);
        assertThat(saved.getValue().getCompanyName()).isEqualTo("Acme");
        assertThat(saved.getValue().getJobTitle()).isEqualTo("Developer");
        assertThat(saved.getValue().getLocation()).isNull();
        assertThat(saved.getValue().getRecruiterEmail()).isNull();
        assertThat(saved.getValue().getNotes()).isNull();
        assertThat(response.companyName()).isEqualTo("Acme");
    }

    @Test
    void updateChangesFieldsOfOwnedApplication() {
        JobApplication existing = application("Old Name");
        when(applicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(existing)).thenReturn(existing);

        ApplicationResponse response = service.update(USER_ID, APPLICATION_ID, request("New Name"));

        assertThat(existing.getCompanyName()).isEqualTo("New Name");
        assertThat(response.companyName()).isEqualTo("New Name");
    }

    @Test
    void updateOfSomeoneElsesApplicationIsNotFoundAndNothingIsSaved() {
        when(applicationRepository.findByIdAndUserId(APPLICATION_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(OTHER_USER_ID, APPLICATION_ID, request("Hacked")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(applicationRepository, never()).save(any(JobApplication.class));
    }

    @Test
    void deleteRemovesOwnedApplication() {
        JobApplication existing = application("Acme");
        when(applicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID)).thenReturn(Optional.of(existing));

        service.delete(USER_ID, APPLICATION_ID);

        verify(applicationRepository).delete(existing);
    }

    @Test
    void deleteOfSomeoneElsesApplicationIsNotFoundAndNothingIsDeleted() {
        when(applicationRepository.findByIdAndUserId(APPLICATION_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(OTHER_USER_ID, APPLICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(applicationRepository, never()).delete(any(JobApplication.class));
    }

    @Test
    void searchClampsPagingAndSortsOldestFirst() {
        when(applicationRepository.findAll(ArgumentMatchers.<Specification<JobApplication>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(application("Acme"))));

        PageResponse<ApplicationSummaryResponse> result =
                service.search(USER_ID, emptyCriteria(), "oldest", -3, 1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(applicationRepository).findAll(ArgumentMatchers.<Specification<JobApplication>>any(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(JobApplicationService.MAX_PAGE_SIZE);
        Sort.Order order = pageable.getValue().getSort().getOrderFor("applicationDate");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(result.content()).hasSize(1);
    }

    @Test
    void searchDefaultsToNewestFirst() {
        when(applicationRepository.findAll(ArgumentMatchers.<Specification<JobApplication>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.search(USER_ID, emptyCriteria(), null, 0, 0);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(applicationRepository).findAll(ArgumentMatchers.<Specification<JobApplication>>any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(JobApplicationService.DEFAULT_PAGE_SIZE);
        assertThat(pageable.getValue().getSort().getOrderFor("applicationDate").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void searchRejectsUnknownSortValue() {
        assertThatThrownBy(() -> service.search(USER_ID, emptyCriteria(), "random", 0, 10))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void searchRejectsInvertedDateRange() {
        ApplicationSearchCriteria criteria = new ApplicationSearchCriteria(null, null, null,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1));

        assertThatThrownBy(() -> service.search(USER_ID, criteria, "newest", 0, 10))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void exportCsvContainsOnlyRowsReturnedForTheUser() {
        when(applicationRepository.findByUserIdOrderByApplicationDateDescIdDesc(USER_ID))
                .thenReturn(List.of(application("Acme")));

        String csv = service.exportCsv(USER_ID);

        assertThat(csv).contains("Company,Job Title").contains("Acme");
        verify(applicationRepository).findByUserIdOrderByApplicationDateDescIdDesc(USER_ID);
    }

    // ---- helpers -------------------------------------------------------------------------

    private static ApplicationSearchCriteria emptyCriteria() {
        return new ApplicationSearchCriteria(null, null, null, null, null);
    }

    private static ApplicationRequest request(String company) {
        return new ApplicationRequest(company, "Developer", null, null, JobType.FULL_TIME,
                LocalDate.of(2026, 11, 1), ApplicationStatus.APPLIED, null, null, null, null, null);
    }

    private static JobApplication application(String company) {
        JobApplication application = new JobApplication();
        ReflectionTestUtils.setField(application, "id", APPLICATION_ID);
        application.setCompanyName(company);
        application.setJobTitle("Developer");
        application.setJobType(JobType.FULL_TIME);
        application.setApplicationDate(LocalDate.of(2026, 11, 1));
        application.setStatus(ApplicationStatus.APPLIED);
        return application;
    }
}
