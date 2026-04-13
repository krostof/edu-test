package com.edutest.persistance.repository;

import com.edutest.persistance.entity.assigment.common.ChoiceOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChoiceOptionJpaRepository extends JpaRepository<ChoiceOptionEntity, Long> {

    @Query("SELECT c FROM ChoiceOptionEntity c WHERE c.assignmentEntity.id = :assignmentId ORDER BY c.orderNumber")
    List<ChoiceOptionEntity> findByAssignmentIdOrderByOrderNumber(@Param("assignmentId") Long assignmentId);

    @Query("SELECT c FROM ChoiceOptionEntity c WHERE c.id IN :ids")
    List<ChoiceOptionEntity> findAllByIds(@Param("ids") List<Long> ids);

    @Query("SELECT c FROM ChoiceOptionEntity c WHERE c.assignmentEntity.id = :assignmentId AND c.isCorrect = true")
    List<ChoiceOptionEntity> findCorrectOptionsByAssignmentId(@Param("assignmentId") Long assignmentId);
}
