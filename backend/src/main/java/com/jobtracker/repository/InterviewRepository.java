package com.jobtracker.repository;

import com.jobtracker.entity.Interview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    @Query("select i from Interview i join fetch i.application a "
            + "where a.id = :applicationId and a.user.id = :userId "
            + "order by i.scheduledAt asc")
    List<Interview> findByApplication(@Param("applicationId") Long applicationId,
                                      @Param("userId") Long userId);

    @Query("select i from Interview i join fetch i.application a "
            + "where a.user.id = :userId order by i.scheduledAt desc")
    List<Interview> findAllByUser(@Param("userId") Long userId);

    @Query("select i from Interview i join fetch i.application a "
            + "where a.user.id = :userId and i.scheduledAt >= :now "
            + "order by i.scheduledAt asc")
    List<Interview> findUpcomingByUser(@Param("userId") Long userId,
                                       @Param("now") LocalDateTime now,
                                       Pageable pageable);

    @Query("select count(i) from Interview i "
            + "where i.application.user.id = :userId and i.scheduledAt >= :now")
    long countUpcomingByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Ownership-aware lookup through the parent application. */
    @Query("select i from Interview i join fetch i.application a "
            + "where i.id = :id and a.user.id = :userId")
    Optional<Interview> findByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);
}
