package com.edutest.persistance.repository;

import com.edutest.domain.group.StudentGroup;
import com.edutest.domain.user.User;
import com.edutest.domain.user.UserRole;
import com.edutest.persistance.entity.group.StudentGroupEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StudentGroupRepositoryImpl implements StudentGroupRepository {

    private final StudentGroupJpaRepository jpaRepository;
    private final UserRepository userRepository;

    @Override
    public StudentGroup save(StudentGroup studentGroup) {
        StudentGroupEntity entity;

        if (studentGroup.getId() != null) {
            entity = jpaRepository.findById(studentGroup.getId())
                    .orElse(new StudentGroupEntity());
        } else {
            entity = new StudentGroupEntity();
        }

        entity.setName(studentGroup.getName());
        entity.setDescription(studentGroup.getDescription());

        if (studentGroup.getId() != null) {
            entity.setId(studentGroup.getId());
        }

        // Map teachers
        List<UserEntity> teacherEntities = new ArrayList<>();
        for (User teacher : studentGroup.getTeachers()) {
            UserEntity teacherEntity = userRepository.findById(teacher.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacher.getId()));
            teacherEntities.add(teacherEntity);
        }
        entity.getTeachers().clear();
        entity.getTeachers().addAll(teacherEntities);

        StudentGroupEntity savedEntity = jpaRepository.save(entity);
        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<StudentGroup> findById(Long id) {
        return jpaRepository.findById(id)
                .map(this::mapToDomain);
    }

    @Override
    public List<StudentGroup> findByTeacher(User teacher) {
        UserEntity teacherEntity = userRepository.findById(teacher.getId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        return jpaRepository.findByTeacher(teacherEntity)
                .stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public Page<StudentGroup> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable)
                .map(this::mapToDomain);
    }

    @Override
    public Page<StudentGroup> findByNameOrDescriptionContaining(String search, Pageable pageable) {
        return jpaRepository.findByNameOrDescriptionContaining(search, pageable)
                .map(this::mapToDomain);
    }

    @Override
    public Page<StudentGroup> findByTeacher(User teacher, Pageable pageable) {
        UserEntity teacherEntity = userRepository.findById(teacher.getId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        return jpaRepository.findByTeacher(teacherEntity, pageable)
                .map(this::mapToDomain);
    }

    @Override
    public Optional<StudentGroup> findByStudent(User student) {
        UserEntity studentEntity = userRepository.findById(student.getId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        return jpaRepository.findByStudent(studentEntity)
                .map(this::mapToDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private StudentGroup mapToDomain(StudentGroupEntity entity) {
        List<User> teachers = new ArrayList<>();
        if (entity.getTeachers() != null) {
            for (UserEntity teacherEntity : entity.getTeachers()) {
                teachers.add(mapUserEntityToDomain(teacherEntity));
            }
        }

        List<User> students = new ArrayList<>();
        if (entity.getStudents() != null) {
            for (UserEntity studentEntity : entity.getStudents()) {
                students.add(mapUserEntityToDomain(studentEntity));
            }
        }

        StudentGroup domain = StudentGroup.builder()
                .name(entity.getName())
                .description(entity.getDescription())
                .teachers(teachers)
                .students(students)
                .build();

        domain.setId(entity.getId());

        return domain;
    }

    private User mapUserEntityToDomain(UserEntity entity) {
        Set<UserRole> roles = entity.getRoles() != null
                ? entity.getRoles().stream()
                    .map(this::mapToDomainRole)
                    .collect(Collectors.toSet())
                : new HashSet<>();

        User user = User.builder()
                .username(entity.getUsername())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .roles(roles)
                .isActive(entity.getIsActive())
                .studentNumber(entity.getStudentNumber())
                .build();

        user.setId(entity.getId());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());

        return user;
    }

    private UserRole mapToDomainRole(UserEntityRole entityRole) {
        if (entityRole == null) return null;
        return switch (entityRole) {
            case STUDENT -> UserRole.STUDENT;
            case TEACHER -> UserRole.TEACHER;
            case ADMIN -> UserRole.ADMIN;
        };
    }
}
