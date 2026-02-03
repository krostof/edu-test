package com.edutest.persistance.repository;

import com.edutest.domain.test.Test;
import com.edutest.domain.user.User;
import com.edutest.domain.group.StudentGroup;
import com.edutest.persistance.entity.test.TestEntity;
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
 * Domain repository interface for Test.
 * Implementation should be provided in infrastructure layer.
 */
@Repository
public interface TestRepository extends JpaRepository<TestEntity, Integer> {


    Test save(Test test);
    
    Optional<Test> findById(Long id);
    
    List<TestEntity> findAll();
    
    Page<TestEntity> findAll(Pageable pageable);
    
    List<Test> findByCreatedBy(User createdBy);
    
    Page<Test> findByCreatedBy(User createdBy, Pageable pageable);
    
    @Query("SELECT t FROM TestEntity t WHERE t.startDate <= CURRENT_TIMESTAMP AND t.endDate >= CURRENT_TIMESTAMP")
    List<Test> findActiveTests();
    
    @Query("SELECT t FROM TestEntity t WHERE t.startDate > CURRENT_TIMESTAMP")
    List<Test> findUpcomingTests();
    
    @Query("SELECT t FROM TestEntity t WHERE t.endDate < CURRENT_TIMESTAMP")
    List<Test> findExpiredTests();
    
    @Query("SELECT t FROM TestEntity t WHERE t.startDate >= :startDate AND t.endDate <= :endDate")
    List<Test> findTestsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT DISTINCT t FROM TestEntity t JOIN t.assignedGroups g JOIN g.members m WHERE m.student.id = :#{#student.id} AND t.startDate <= CURRENT_TIMESTAMP AND t.endDate >= CURRENT_TIMESTAMP")
    List<Test> findAvailableTestsForStudent(@Param("student") User student);

    @Query("SELECT t FROM TestEntity t JOIN t.assignedGroups g WHERE g.id = :#{#group.id}")
    List<Test> findTestsByGroup(@Param("group") StudentGroup group);

    @Query("SELECT t FROM TestEntity t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Test> findByTitleOrDescriptionContaining(@Param("searchTerm") String searchTerm, Pageable pageable);

    boolean existsById(Long id);
    
    boolean existsByTitleAndCreatedBy(String title, User createdBy);
    
    void deleteById(Long id);
    
    long count();
    
    long countByCreatedBy(User createdBy);
}