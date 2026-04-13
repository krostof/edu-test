package com.edutest.persistance.repository;

import com.edutest.persistance.entity.assigment.common.AssignmentAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentAnswerJpaRepository extends JpaRepository<AssignmentAnswerEntity, Long> {

    @Query("SELECT a FROM AssignmentAnswerEntity a WHERE a.testAttemptEntity.id = :attemptId AND a.assignmentEntity.id = :assignmentId")
    Optional<AssignmentAnswerEntity> findByTestAttemptIdAndAssignmentId(
            @Param("attemptId") Long attemptId,
            @Param("assignmentId") Long assignmentId);

    @Query("SELECT a FROM AssignmentAnswerEntity a WHERE a.testAttemptEntity.id = :attemptId")
    List<AssignmentAnswerEntity> findByTestAttemptId(@Param("attemptId") Long attemptId);

    @Query("SELECT a FROM AssignmentAnswerEntity a WHERE a.testAttemptEntity.id = :attemptId AND a.isGraded = false")
    List<AssignmentAnswerEntity> findUngradedByTestAttemptId(@Param("attemptId") Long attemptId);

    @Query("SELECT a FROM AssignmentAnswerEntity a WHERE a.student.id = :studentId")
    List<AssignmentAnswerEntity> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(a) FROM AssignmentAnswerEntity a WHERE a.testAttemptEntity.id = :attemptId AND a.isGraded = true")
    long countGradedByTestAttemptId(@Param("attemptId") Long attemptId);

    @Query("SELECT COUNT(a) FROM AssignmentAnswerEntity a WHERE a.testAttemptEntity.id = :attemptId AND a.isGraded = false")
    long countUngradedByTestAttemptId(@Param("attemptId") Long attemptId);

    @Query("SELECT SUM(a.score) FROM AssignmentAnswerEntity a WHERE a.testAttemptEntity.id = :attemptId AND a.isGraded = true")
    Float sumScoresByTestAttemptId(@Param("attemptId") Long attemptId);

    boolean existsByTestAttemptEntityIdAndAssignmentEntityId(Long attemptId, Long assignmentId);
}
