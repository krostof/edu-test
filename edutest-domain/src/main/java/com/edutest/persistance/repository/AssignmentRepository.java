package com.edutest.persistance.repository;

import com.edutest.domain.assignment.Assignment;
import com.edutest.domain.assignment.AssignmentType;
import com.edutest.domain.test.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for Assignment.
 * Implementation should be provided in infrastructure layer.
 */
@Repository
public interface AssignmentRepository {

    Assignment save(Assignment assignment);
    
    Optional<Assignment> findById(Long id);
    
    List<Assignment> findAll();
    
    Page<Assignment> findAll(Pageable pageable);
    
    List<Assignment> findByTest(Test test);
    
    List<Assignment> findByTestId(Long testId);
    
    List<Assignment> findByTestOrderByOrderNumber(Test test);
    
    List<Assignment> findByTestIdOrderByOrderNumber(Long testId);
    
    List<Assignment> findByType(AssignmentType type);
    
    Page<Assignment> findByType(AssignmentType type, Pageable pageable);
    
    List<Assignment> findByTestAndType(Test test, AssignmentType type);
    
    List<Assignment> findByTestIdAndType(Long testId, AssignmentType type);
    
    Optional<Assignment> findByTestIdAndOrderNumber(Long testId, Integer orderNumber);
    
    Page<Assignment> findByTitleOrDescriptionContaining(String searchTerm, Pageable pageable);
    
    boolean existsById(Long id);
    
    boolean existsByTestIdAndOrderNumber(Long testId, Integer orderNumber);
    
    boolean existsByTestIdAndTitle(Long testId, String title);
    
    void deleteById(Long id);
    
    void deleteByTestId(Long testId);
    
    long count();
    
    long countByTestId(Long testId);
    
    long countByType(AssignmentType type);
    
    Float sumPointsByTestId(Long testId);
    
    Integer getMaxOrderNumberByTestId(Long testId);
}