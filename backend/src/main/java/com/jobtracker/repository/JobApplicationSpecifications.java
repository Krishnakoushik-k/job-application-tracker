package com.jobtracker.repository;

import com.jobtracker.entity.JobApplication;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.enums.JobType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Composable query fragments for the application list. Every query starts with
 * {@link #ownedBy(Long)} so a user can never see another user's rows.
 */
public final class JobApplicationSpecifications {

    private static final char LIKE_ESCAPE = '!';

    private JobApplicationSpecifications() {
    }

    public static Specification<JobApplication> ownedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    /** Case-insensitive "contains" match on company name OR job title. */
    public static Specification<JobApplication> matchesSearch(String term) {
        if (term == null || term.isBlank()) {
            return noFilter();
        }
        String pattern = "%" + escapeLike(term.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("companyName")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("jobTitle")), pattern, LIKE_ESCAPE));
    }

    public static Specification<JobApplication> hasStatus(ApplicationStatus status) {
        if (status == null) {
            return noFilter();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<JobApplication> hasJobType(JobType jobType) {
        if (jobType == null) {
            return noFilter();
        }
        return (root, query, cb) -> cb.equal(root.get("jobType"), jobType);
    }

    public static Specification<JobApplication> appliedOnOrAfter(LocalDate from) {
        if (from == null) {
            return noFilter();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("applicationDate"), from);
    }

    public static Specification<JobApplication> appliedOnOrBefore(LocalDate to) {
        if (to == null) {
            return noFilter();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("applicationDate"), to);
    }

    private static Specification<JobApplication> noFilter() {
        return (root, query, cb) -> cb.conjunction();
    }

    /** Escapes LIKE wildcards so "100%" is matched literally instead of as a pattern. */
    static String escapeLike(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
