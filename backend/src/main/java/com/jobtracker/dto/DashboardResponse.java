package com.jobtracker.dto;

import com.jobtracker.enums.ApplicationStatus;

import java.util.List;

/**
 * Dashboard statistics for the authenticated user.
 * successRate = offers / total applications * 100 (one decimal place, 0 when there are no applications).
 */
public record DashboardResponse(
        long totalApplications,
        long appliedCount,
        long assessmentCount,
        long interviewCount,
        long offerCount,
        long rejectedCount,
        long withdrawnCount,
        long upcomingInterviewCount,
        double successRate,
        List<StatusCount> statusDistribution,
        List<MonthlyCount> monthlyTrend,
        List<InterviewResponse> upcomingInterviews,
        List<ApplicationSummaryResponse> recentApplications
) {

    public record StatusCount(ApplicationStatus status, long count) {
    }

    /** month is formatted as yyyy-MM, e.g. 2026-11. */
    public record MonthlyCount(String month, long count) {
    }
}
