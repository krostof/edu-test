package com.edutest.persistance.repository;

import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeSubmissionJpaRepository extends JpaRepository<CodeSubmissionEntity, Long> {

    @Query("SELECT c FROM CodeSubmissionEntity c WHERE c.testAttempt.id = :attemptId AND c.assignment.id = :assignmentId")
    Optional<CodeSubmissionEntity> findByTestAttemptIdAndAssignmentId(
            @Param("attemptId") Long attemptId,
            @Param("assignmentId") Long assignmentId);

    @Query("SELECT c FROM CodeSubmissionEntity c WHERE c.testAttempt.id = :attemptId")
    List<CodeSubmissionEntity> findByTestAttemptId(@Param("attemptId") Long attemptId);

    @Query("SELECT c FROM CodeSubmissionEntity c WHERE c.student.id = :studentId")
    List<CodeSubmissionEntity> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT c FROM CodeSubmissionEntity c LEFT JOIN FETCH c.testCaseResults WHERE c.id = :id")
    Optional<CodeSubmissionEntity> findByIdWithTestCaseResults(@Param("id") Long id);

    @Query("SELECT c FROM CodeSubmissionEntity c LEFT JOIN FETCH c.testCaseResults WHERE c.testAttempt.id = :attemptId AND c.assignment.id = :assignmentId")
    Optional<CodeSubmissionEntity> findByTestAttemptIdAndAssignmentIdWithTestCaseResults(
            @Param("attemptId") Long attemptId,
            @Param("assignmentId") Long assignmentId);

    @Query("SELECT SUM(c.totalScore) FROM CodeSubmissionEntity c WHERE c.testAttempt.id = :attemptId")
    Float sumScoresByTestAttemptId(@Param("attemptId") Long attemptId);

    boolean existsByTestAttemptIdAndAssignmentId(Long attemptId, Long assignmentId);
}
