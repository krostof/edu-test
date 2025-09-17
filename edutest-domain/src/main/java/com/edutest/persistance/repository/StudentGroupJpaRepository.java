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

    List<StudentGroupEntity> findByTeacher(UserEntity teacher);

    @Query("SELECT sg FROM StudentGroupEntity sg WHERE sg.name ILIKE %:search% OR sg.description ILIKE %:search%")
    Page<StudentGroupEntity> findByNameOrDescriptionContaining(@Param("search") String search, Pageable pageable);

    Page<StudentGroupEntity> findByTeacher(UserEntity teacher, Pageable pageable);

    @Query("SELECT sg FROM StudentGroupEntity sg JOIN sg.members m WHERE m.student = :student")
    List<StudentGroupEntity> findByStudent(@Param("student") UserEntity student);

    Optional<StudentGroupEntity> findByNameAndTeacher(String name, UserEntity teacher);

    boolean existsByNameAndTeacher(String name, UserEntity teacher);
}