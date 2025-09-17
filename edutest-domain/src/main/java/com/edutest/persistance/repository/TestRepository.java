package com.edutest.persistance.repository;

import com.edutest.domain.test.Test;
import com.edutest.domain.user.User;
import com.edutest.domain.group.StudentGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for Test.
 * Implementation should be provided in infrastructure layer.
 */
@Repository
public interface TestRepository {

    Test save(Test test);
    
    Optional<Test> findById(Long id);
    
    List<Test> findAll();
    
    Page<Test> findAll(Pageable pageable);
    
    List<Test> findByCreatedBy(User createdBy);
    
    Page<Test> findByCreatedBy(User createdBy, Pageable pageable);
    
    List<Test> findActiveTests();
    
    List<Test> findUpcomingTests();
    
    List<Test> findExpiredTests();
    
    List<Test> findTestsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Test> findAvailableTestsForStudent(User student);
    
    List<Test> findTestsByGroup(StudentGroup group);
    
    Page<Test> findByTitleOrDescriptionContaining(String searchTerm, Pageable pageable);
    
    boolean existsById(Long id);
    
    boolean existsByTitleAndCreatedBy(String title, User createdBy);
    
    void deleteById(Long id);
    
    long count();
    
    long countByCreatedBy(User createdBy);
}