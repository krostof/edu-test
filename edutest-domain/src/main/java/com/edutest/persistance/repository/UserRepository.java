package com.edutest.persistance.repository;

import com.edutest.persistance.entity.group.StudentGroupEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for User.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM UserEntity u WHERE :role MEMBER OF u.roles")
    Page<UserEntity> findByRole(@Param("role") UserEntityRole role, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT sg) FROM StudentGroupEntity sg JOIN sg.teachers t WHERE t.id = :teacherId")
    long countGroupsByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT u FROM UserEntity u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<UserEntity> searchUsers(@Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE :role MEMBER OF u.roles AND (" +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<UserEntity> searchUsersByRole(@Param("search") String search, @Param("role") UserEntityRole role, Pageable pageable);

    List<UserEntity> findByStudentGroup(StudentGroupEntity group);

    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE u.studentGroup.id = :groupId AND r = 'STUDENT'")
    List<UserEntity> findStudentsByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE u.studentGroup IS NULL AND r = 'STUDENT'")
    List<UserEntity> findStudentsWithoutGroup();
}