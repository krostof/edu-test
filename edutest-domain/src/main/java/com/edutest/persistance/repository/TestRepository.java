package com.edutest.persistance.repository;

import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository interface for TestEntity.
 * Returns entity types that should be mapped to domain models in the service layer.
 */
@Repository
public interface TestRepository extends JpaRepository<TestEntity, Long> {

    Optional<TestEntity> findById(Long id);

    List<TestEntity> findAll();

    Page<TestEntity> findAll(Pageable pageable);

    List<TestEntity> findByCreatedBy(UserEntity createdBy);

    Page<TestEntity> findByCreatedBy(UserEntity createdBy, Pageable pageable);

    @Query("SELECT t FROM TestEntity t WHERE t.startDate <= CURRENT_TIMESTAMP AND t.endDate >= CURRENT_TIMESTAMP")
    List<TestEntity> findActiveTests();

    @Query("SELECT t FROM TestEntity t WHERE t.startDate > CURRENT_TIMESTAMP")
    List<TestEntity> findUpcomingTests();

    @Query("SELECT t FROM TestEntity t WHERE t.endDate < CURRENT_TIMESTAMP")
    List<TestEntity> findExpiredTests();

    @Query("SELECT t FROM TestEntity t WHERE t.startDate >= :startDate AND t.endDate <= :endDate")
    List<TestEntity> findTestsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT DISTINCT t FROM TestEntity t JOIN t.assignedGroups g JOIN g.students s WHERE s.id = :studentId AND t.startDate <= CURRENT_TIMESTAMP AND t.endDate >= CURRENT_TIMESTAMP")
    List<TestEntity> findAvailableTestsForStudent(@Param("studentId") Long studentId);

    @Query("SELECT t FROM TestEntity t JOIN t.assignedGroups g WHERE g.id = :groupId")
    List<TestEntity> findTestsByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT t FROM TestEntity t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<TestEntity> findByTitleOrDescriptionContaining(@Param("searchTerm") String searchTerm, Pageable pageable);

    boolean existsById(Long id);

    boolean existsByTitleAndCreatedBy(String title, UserEntity createdBy);

    void deleteById(Long id);

    long count();

    long countByCreatedBy(UserEntity createdBy);
}