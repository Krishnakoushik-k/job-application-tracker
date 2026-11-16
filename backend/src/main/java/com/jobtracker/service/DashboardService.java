package com.jobtracker.service;

import com.jobtracker.dto.ApplicationSummaryResponse;
import com.jobtracker.dto.DashboardResponse;
import com.jobtracker.dto.DashboardResponse.MonthlyCount;
import com.jobtracker.dto.DashboardResponse.StatusCount;
import com.jobtracker.dto.InterviewResponse;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.mapper.ApplicationMapper;
import com.jobtracker.mapper.InterviewMapper;
import com.jobtracker.repository.InterviewRepository;
import com.jobtracker.repository.JobApplicationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    static final int TREND_MONTHS = 12;
    static final int RECENT_LIMIT = 5;
    static final int UPCOMING_LIMIT = 5;

    private final JobApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final ApplicationMapper applicationMapper;
    private final InterviewMapper interviewMapper;
    private final Clock clock;

    public DashboardService(JobApplicationRepository applicationRepository,
                            InterviewRepository interviewRepository,
                            ApplicationMapper applicationMapper,
                            InterviewMapper interviewMapper,
                            Clock clock) {
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
        this.applicationMapper = applicationMapper;
        this.interviewMapper = interviewMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getStatistics(Long userId) {
        Map<ApplicationStatus, Long> counts = loadStatusCounts(userId);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long offers = counts.get(ApplicationStatus.OFFER);

        List<StatusCount> distribution = new ArrayList<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            distribution.add(new StatusCount(status, counts.get(status)));
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<InterviewResponse> upcoming = interviewRepository
                .findUpcomingByUser(userId, now, PageRequest.of(0, UPCOMING_LIMIT))
                .stream()
                .map(interviewMapper::toResponse)
                .toList();

        List<ApplicationSummaryResponse> recent = applicationRepository
                .findByUserId(userId, PageRequest.of(0, RECENT_LIMIT,
                        Sort.by(Sort.Direction.DESC, "applicationDate", "id")))
                .stream()
                .map(applicationMapper::toSummary)
                .toList();

        return new DashboardResponse(
                total,
                counts.get(ApplicationStatus.APPLIED),
                counts.get(ApplicationStatus.ASSESSMENT),
                counts.get(ApplicationStatus.INTERVIEW),
                offers,
                counts.get(ApplicationStatus.REJECTED),
                counts.get(ApplicationStatus.WITHDRAWN),
                interviewRepository.countUpcomingByUser(userId, now),
                calculateSuccessRate(offers, total),
                distribution,
                loadMonthlyTrend(userId),
                upcoming,
                recent);
    }

    /** Offers divided by all applications, as a percentage rounded to one decimal place. */
    static double calculateSuccessRate(long offers, long total) {
        if (total == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(offers * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Map<ApplicationStatus, Long> loadStatusCounts(Long userId) {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : applicationRepository.countByStatus(userId)) {
            counts.put((ApplicationStatus) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    /** Always returns the last 12 months (oldest first), with 0 for months without applications. */
    private List<MonthlyCount> loadMonthlyTrend(Long userId) {
        YearMonth current = YearMonth.now(clock);
        YearMonth first = current.minusMonths(TREND_MONTHS - 1L);
        LocalDate from = first.atDay(1);

        Map<YearMonth, Long> byMonth = new HashMap<>();
        for (Object[] row : applicationRepository.countByMonthSince(userId, from)) {
            YearMonth month = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            byMonth.put(month, ((Number) row[2]).longValue());
        }

        List<MonthlyCount> trend = new ArrayList<>();
        for (int i = 0; i < TREND_MONTHS; i++) {
            YearMonth month = first.plusMonths(i);
            trend.add(new MonthlyCount(month.toString(), byMonth.getOrDefault(month, 0L)));
        }
        return trend;
    }
}
