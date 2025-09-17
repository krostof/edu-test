package com.edutest.persistance.repository;

import com.edutest.domain.user.User;
import com.edutest.domain.user.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Domain repository interface for User.
 * Implementation should be provided in infrastructure layer.
 */
public interface UserRepository {

    User save(User user);
    
    Optional<User> findById(Long id);
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    Page<User> findByRole(UserRole role, Pageable pageable);
    
    Page<User> findAll(Pageable pageable);
    
    boolean existsById(Long id);
    
    void deleteById(Long id);
}