package com.edutest.persistance.repository;

import com.edutest.persistance.entity.group.StudentGroupEntity;
import com.edutest.persistance.entity.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentGroupJpaRepository extends JpaRepository<StudentGroupEntity, Long> {

    // JPQL override so @SQLRestriction("deleted_at IS NULL") applies to fetch-by-id as well
    // (find() by primary key bypasses it). Soft-deleted groups stay hidden everywhere.
    @Query("SELECT sg FROM StudentGroupEntity sg WHERE sg.id = :id")
    Optional<StudentGroupEntity> findById(@Param("id") Long id);

    @Query("SELECT sg FROM StudentGroupEntity sg JOIN sg.teachers t WHERE t = :teacher")
    List<StudentGroupEntity> findByTeacher(@Param("teacher") UserEntity teacher);

    @Query("SELECT sg FROM StudentGroupEntity sg WHERE sg.name ILIKE %:search% OR sg.description ILIKE %:search%")
    Page<StudentGroupEntity> findByNameOrDescriptionContaining(@Param("search") String search, Pageable pageable);

    @Query("SELECT sg FROM StudentGroupEntity sg JOIN sg.teachers t WHERE t = :teacher")
    Page<StudentGroupEntity> findByTeacher(@Param("teacher") UserEntity teacher, Pageable pageable);

    @Query("SELECT u.studentGroup FROM UserEntity u WHERE u = :student")
    Optional<StudentGroupEntity> findByStudent(@Param("student") UserEntity student);

    boolean existsByName(String name);

    /**
     * Removes all test-group assignment links for a group. Used when soft-deleting a group
     * so it no longer appears among any test's assigned groups. Native delete on the join table.
     */
    @Modifying
    @Query(value = "DELETE FROM test_groups WHERE group_id = :groupId", nativeQuery = true)
    void removeGroupFromAllTests(@Param("groupId") Long groupId);
}
