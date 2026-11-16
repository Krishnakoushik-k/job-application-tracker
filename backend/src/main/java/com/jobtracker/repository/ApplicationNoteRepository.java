package com.jobtracker.repository;

import com.jobtracker.entity.ApplicationNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationNoteRepository extends JpaRepository<ApplicationNote, Long> {

    @Query("select n from ApplicationNote n join fetch n.application a "
            + "where a.id = :applicationId and a.user.id = :userId "
            + "order by n.createdAt desc, n.id desc")
    List<ApplicationNote> findByApplication(@Param("applicationId") Long applicationId,
                                            @Param("userId") Long userId);

    /** Ownership-aware lookup through the parent application. */
    @Query("select n from ApplicationNote n join fetch n.application a "
            + "where n.id = :id and a.user.id = :userId")
    Optional<ApplicationNote> findByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);
}
