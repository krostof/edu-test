package com.edutest.persistance.repository;

import com.edutest.domain.group.StudentGroup;
import com.edutest.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for StudentGroup.
 * Implementation should be provided in infrastructure layer.
 */
public interface StudentGroupRepository {

    StudentGroup save(StudentGroup studentGroup);
    
    Optional<StudentGroup> findById(Long id);
    
    List<StudentGroup> findByTeacher(User teacher);
    
    Page<StudentGroup> findAll(Pageable pageable);
    
    Page<StudentGroup> findByNameOrDescriptionContaining(String search, Pageable pageable);
    
    Page<StudentGroup> findByTeacher(User teacher, Pageable pageable);
    
    List<StudentGroup> findByStudent(User student);
    
    Optional<StudentGroup> findByNameAndTeacher(String name, User teacher);
    
    boolean existsByNameAndTeacher(String name, User teacher);
    
    boolean existsById(Long id);
    
    void deleteById(Long id);
}