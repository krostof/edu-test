package com.edutest.persistance.repository;

import com.edutest.persistance.entity.test.TestAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestAttemptJpaRepository extends JpaRepository<TestAttemptEntity, Long> {

    @Query("SELECT t FROM TestAttemptEntity t WHERE t.id = :attemptId AND t.testEntity.id = :testId")
    Optional<TestAttemptEntity> findByIdAndTestId(@Param("attemptId") Long attemptId, @Param("testId") Long testId);

    @Query("SELECT t FROM TestAttemptEntity t WHERE t.id = :attemptId AND t.student.id = :studentId")
    Optional<TestAttemptEntity> findByIdAndStudentId(@Param("attemptId") Long attemptId, @Param("studentId") Long studentId);

    @Query("SELECT t FROM TestAttemptEntity t WHERE t.testEntity.id = :testId AND t.student.id = :studentId")
    Optional<TestAttemptEntity> findByTestIdAndStudentId(@Param("testId") Long testId, @Param("studentId") Long studentId);

    @Query("SELECT t FROM TestAttemptEntity t WHERE t.testEntity.id = :testId")
    List<TestAttemptEntity> findByTestId(@Param("testId") Long testId);

    @Query("SELECT t FROM TestAttemptEntity t WHERE t.student.id = :studentId")
    List<TestAttemptEntity> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT t FROM TestAttemptEntity t WHERE t.testEntity.id = :testId AND t.isCompleted = true")
    List<TestAttemptEntity> findCompletedByTestId(@Param("testId") Long testId);

    @Query("SELECT t FROM TestAttemptEntity t WHERE t.testEntity.id = :testId AND t.isCompleted = false")
    List<TestAttemptEntity> findInProgressByTestId(@Param("testId") Long testId);

    @Query("SELECT t FROM TestAttemptEntity t JOIN FETCH t.testEntity WHERE t.id = :attemptId")
    Optional<TestAttemptEntity> findByIdWithTest(@Param("attemptId") Long attemptId);

    @Query("SELECT t FROM TestAttemptEntity t JOIN FETCH t.testEntity JOIN FETCH t.student WHERE t.id = :attemptId")
    Optional<TestAttemptEntity> findByIdWithTestAndStudent(@Param("attemptId") Long attemptId);

    boolean existsByTestEntityIdAndStudentId(Long testId, Long studentId);

    long countByStudentId(Long studentId);

    long countByTestEntityId(Long testId);

    long countByTestEntityIdAndIsCompleted(Long testId, Boolean isCompleted);
}
