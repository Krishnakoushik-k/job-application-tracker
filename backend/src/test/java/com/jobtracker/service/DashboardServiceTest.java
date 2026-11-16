package com.jobtracker.service;

import com.jobtracker.dto.DashboardResponse;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.mapper.ApplicationMapper;
import com.jobtracker.mapper.InterviewMapper;
import com.jobtracker.repository.InterviewRepository;
import com.jobtracker.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Long USER_ID = 1L;
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private InterviewRepository interviewRepository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(applicationRepository, interviewRepository,
                new ApplicationMapper(), new InterviewMapper(), FIXED_CLOCK);
    }

    @Test
    void calculatesCountsSuccessRateAndTwelveMonthTrend() {
        when(applicationRepository.countByStatus(USER_ID)).thenReturn(List.<Object[]>of(
                new Object[]{ApplicationStatus.APPLIED, 3L},
                new Object[]{ApplicationStatus.OFFER, 1L},
                new Object[]{ApplicationStatus.REJECTED, 1L}));
        when(applicationRepository.countByMonthSince(USER_ID, LocalDate.of(2025, 12, 1))).thenReturn(List.<Object[]>of(
                new Object[]{2026, 11, 2L},
                new Object[]{2026, 10, 3L}));
        when(interviewRepository.findUpcomingByUser(eq(USER_ID), eq(LocalDateTime.of(2026, 11, 10, 10, 0)),
                any(Pageable.class))).thenReturn(List.of());
        when(interviewRepository.countUpcomingByUser(USER_ID, LocalDateTime.of(2026, 11, 10, 10, 0))).thenReturn(2L);
        when(applicationRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

        DashboardResponse stats = service.getStatistics(USER_ID);

        assertThat(stats.totalApplications()).isEqualTo(5);
        assertThat(stats.appliedCount()).isEqualTo(3);
        assertThat(stats.assessmentCount()).isZero();
        assertThat(stats.interviewCount()).isZero();
        assertThat(stats.offerCount()).isEqualTo(1);
        assertThat(stats.rejectedCount()).isEqualTo(1);
        assertThat(stats.withdrawnCount()).isZero();
        assertThat(stats.upcomingInterviewCount()).isEqualTo(2);
        assertThat(stats.successRate()).isEqualTo(20.0);

        assertThat(stats.statusDistribution()).hasSize(ApplicationStatus.values().length);

        assertThat(stats.monthlyTrend()).hasSize(12);
        assertThat(stats.monthlyTrend().get(0).month()).isEqualTo("2025-12");
        assertThat(stats.monthlyTrend().get(11).month()).isEqualTo("2026-11");
        assertThat(stats.monthlyTrend().get(11).count()).isEqualTo(2);
        assertThat(stats.monthlyTrend().get(10).count()).isEqualTo(3);
        assertThat(stats.monthlyTrend().get(0).count()).isZero();
    }

    @Test
    void userWithNoApplicationsGetsZeroesAndZeroSuccessRate() {
        DashboardResponse stats = service.getStatistics(USER_ID);

        assertThat(stats.totalApplications()).isZero();
        assertThat(stats.successRate()).isZero();
        assertThat(stats.statusDistribution()).allSatisfy(entry -> assertThat(entry.count()).isZero());
        assertThat(stats.monthlyTrend()).hasSize(12).allSatisfy(entry -> assertThat(entry.count()).isZero());
        assertThat(stats.upcomingInterviews()).isEmpty();
        assertThat(stats.recentApplications()).isEmpty();
    }

    @Test
    void successRateIsRoundedToOneDecimal() {
        assertThat(DashboardService.calculateSuccessRate(1, 3)).isEqualTo(33.3);
        assertThat(DashboardService.calculateSuccessRate(2, 3)).isEqualTo(66.7);
        assertThat(DashboardService.calculateSuccessRate(3, 3)).isEqualTo(100.0);
        assertThat(DashboardService.calculateSuccessRate(0, 0)).isZero();
    }
}
