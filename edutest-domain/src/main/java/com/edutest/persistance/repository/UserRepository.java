package com.edutest.persistance.repository;

import com.edutest.persistance.entity.group.StudentGroupEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // Overrides the inherited EntityManager.find()-based lookup with JPQL so that
    // @SQLRestriction("deleted_at IS NULL") is honored on fetch-by-id too (find() by
    // primary key bypasses @SQLRestriction; JPQL does not). Keeps soft-deleted users
    // hidden from detail endpoints, not just from list/search queries.
    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findById(@Param("id") Long id);

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

    // Former members of a since-deleted group, used by restoreGroup to re-attach them.
    @Query("SELECT u FROM UserEntity u WHERE u.deletedFromGroupId = :groupId")
    List<UserEntity> findByDeletedFromGroupId(@Param("groupId") Long groupId);

    // Soft-delete restore support. Native SQL bypasses @SQLRestriction (which always hides
    // deleted_at IS NOT NULL rows), so these are the only way to see/restore deleted users.
    @Query(value = "SELECT * FROM users WHERE deleted_at IS NOT NULL", nativeQuery = true)
    List<UserEntity> findAllDeleted();

    @Query(value = "SELECT * FROM users WHERE id = :id AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<UserEntity> findDeletedById(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE users SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void restoreById(@Param("id") Long id);
}