package com.edutest.persistance.repository;

import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.test.TestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for AssignmentEntity.
 * Works with database entities directly.
 */
@Repository
public interface AssignmentJpaRepository extends JpaRepository<AssignmentEntity, Long> {

    List<AssignmentEntity> findByTestEntity(TestEntity testEntity);

    List<AssignmentEntity> findByTestEntityOrderByOrderNumber(TestEntity testEntity);

    List<AssignmentEntity> findByTestEntityIdOrderByOrderNumber(Long testId);

    @Query("SELECT a FROM AssignmentEntity a WHERE a.testEntity.id = :testId AND a.orderNumber = :orderNumber")
    Optional<AssignmentEntity> findByTestIdAndOrderNumber(@Param("testId") Long testId, @Param("orderNumber") Integer orderNumber);

    @Query("SELECT a FROM AssignmentEntity a WHERE a.title ILIKE %:search% OR a.description ILIKE %:search%")
    Page<AssignmentEntity> findByTitleOrDescriptionContaining(@Param("search") String searchTerm, Pageable pageable);

    boolean existsByTestEntityIdAndOrderNumber(Long testId, Integer orderNumber);

    boolean existsByTestEntityIdAndTitle(Long testId, String title);

    void deleteByTestEntityId(Long testId);

    long countByTestEntityId(Long testId);

    @Query("SELECT SUM(a.points) FROM AssignmentEntity a WHERE a.testEntity.id = :testId")
    Float sumPointsByTestId(@Param("testId") Long testId);

    @Query("SELECT MAX(a.orderNumber) FROM AssignmentEntity a WHERE a.testEntity.id = :testId")
    Integer getMaxOrderNumberByTestId(@Param("testId") Long testId);

    @Query("SELECT a FROM AssignmentEntity a WHERE TYPE(a) = :entityClass")
    List<AssignmentEntity> findByType(@Param("entityClass") Class<? extends AssignmentEntity> entityClass);

    @Query("SELECT a FROM AssignmentEntity a WHERE TYPE(a) = :entityClass")
    Page<AssignmentEntity> findByType(@Param("entityClass") Class<? extends AssignmentEntity> entityClass, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AssignmentEntity a WHERE TYPE(a) = :entityClass")
    long countByType(@Param("entityClass") Class<? extends AssignmentEntity> entityClass);
}


