package com.edutest.persistance.repository;

import com.edutest.persistance.entity.attachment.AttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<AttachmentEntity, Long> {

    Optional<AttachmentEntity> findByStoredFilename(String storedFilename);

    @Query("SELECT a FROM AttachmentEntity a WHERE a.testAttempt.id = :attemptId AND a.assignmentId = :assignmentId")
    List<AttachmentEntity> findByAttemptIdAndAssignmentId(
            @Param("attemptId") Long attemptId,
            @Param("assignmentId") Long assignmentId);

    @Query("SELECT a FROM AttachmentEntity a WHERE a.testAttempt.id = :attemptId")
    List<AttachmentEntity> findByAttemptId(@Param("attemptId") Long attemptId);

    @Query("SELECT a FROM AttachmentEntity a WHERE a.uploadedBy.id = :userId")
    List<AttachmentEntity> findByUploadedBy(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) FROM AttachmentEntity a WHERE a.testAttempt.id = :attemptId AND a.assignmentId = :assignmentId")
    long countByAttemptIdAndAssignmentId(
            @Param("attemptId") Long attemptId,
            @Param("assignmentId") Long assignmentId);
}
