package com.jobtracker.repository;

import com.jobtracker.entity.JobApplication;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {

    /** Ownership-aware lookup: returns empty when the application belongs to someone else. */
    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    List<JobApplication> findByUserIdOrderByApplicationDateDescIdDesc(Long userId);

    /** Used for "recent applications"; the page size limits the result. */
    List<JobApplication> findByUserId(Long userId, Pageable pageable);

    @Query("select a.status, count(a) from JobApplication a "
            + "where a.user.id = :userId group by a.status")
    List<Object[]> countByStatus(@Param("userId") Long userId);

    @Query("select extract(year from a.applicationDate), extract(month from a.applicationDate), count(a) "
            + "from JobApplication a "
            + "where a.user.id = :userId and a.applicationDate >= :from "
            + "group by extract(year from a.applicationDate), extract(month from a.applicationDate)")
    List<Object[]> countByMonthSince(@Param("userId") Long userId, @Param("from") LocalDate from);
}
