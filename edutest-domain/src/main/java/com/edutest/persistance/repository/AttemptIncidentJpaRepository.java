package com.edutest.persistance.repository;

import com.edutest.persistance.entity.test.AttemptIncidentEntity;
import com.edutest.persistance.entity.test.IncidentTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptIncidentJpaRepository extends JpaRepository<AttemptIncidentEntity, Long> {

    @Query("SELECT i FROM AttemptIncidentEntity i WHERE i.testAttempt.id = :attemptId ORDER BY i.occurredAt ASC")
    List<AttemptIncidentEntity> findByAttemptIdOrderByOccurredAt(@Param("attemptId") Long attemptId);

    @Query("SELECT COUNT(i) FROM AttemptIncidentEntity i WHERE i.testAttempt.id = :attemptId")
    long countByAttemptId(@Param("attemptId") Long attemptId);

    /**
     * Find the most recent incident of the given type for an attempt — used for backend-side
     * deduplication to avoid log spam (e.g., user toggles tab visibility 30 times in a second).
     */
    @Query("""
            SELECT i FROM AttemptIncidentEntity i
            WHERE i.testAttempt.id = :attemptId AND i.type = :type
              AND i.occurredAt > :since
            ORDER BY i.occurredAt DESC
            """)
    Optional<AttemptIncidentEntity> findLatestByTypeSince(
            @Param("attemptId") Long attemptId,
            @Param("type") IncidentTypeEnum type,
            @Param("since") LocalDateTime since);
}
