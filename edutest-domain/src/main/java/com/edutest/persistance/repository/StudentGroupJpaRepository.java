package com.edutest.persistance.repository;

import com.edutest.persistance.entity.group.StudentGroupEntity;
import com.edutest.persistance.entity.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentGroupJpaRepository extends JpaRepository<StudentGroupEntity, Long> {

    @Query("SELECT sg FROM StudentGroupEntity sg JOIN sg.teachers t WHERE t = :teacher")
    List<StudentGroupEntity> findByTeacher(@Param("teacher") UserEntity teacher);

    @Query("SELECT sg FROM StudentGroupEntity sg WHERE sg.name ILIKE %:search% OR sg.description ILIKE %:search%")
    Page<StudentGroupEntity> findByNameOrDescriptionContaining(@Param("search") String search, Pageable pageable);

    @Query("SELECT sg FROM StudentGroupEntity sg JOIN sg.teachers t WHERE t = :teacher")
    Page<StudentGroupEntity> findByTeacher(@Param("teacher") UserEntity teacher, Pageable pageable);

    @Query("SELECT u.studentGroup FROM UserEntity u WHERE u = :student")
    Optional<StudentGroupEntity> findByStudent(@Param("student") UserEntity student);

    boolean existsByName(String name);
}
