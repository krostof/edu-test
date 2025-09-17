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

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StudentGroupRepositoryImpl implements StudentGroupRepository {

    private final StudentGroupJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public StudentGroup save(StudentGroup studentGroup) {
        StudentGroupEntity entity = mapToEntity(studentGroup);
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
        UserEntity teacherEntity = userJpaRepository.findById(teacher.getId())
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
        UserEntity teacherEntity = userJpaRepository.findById(teacher.getId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        
        return jpaRepository.findByTeacher(teacherEntity, pageable)
                .map(this::mapToDomain);
    }

    @Override
    public List<StudentGroup> findByStudent(User student) {
        UserEntity studentEntity = userJpaRepository.findById(student.getId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        
        return jpaRepository.findByStudent(studentEntity)
                .stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public Optional<StudentGroup> findByNameAndTeacher(String name, User teacher) {
        UserEntity teacherEntity = userJpaRepository.findById(teacher.getId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        
        return jpaRepository.findByNameAndTeacher(name, teacherEntity)
                .map(this::mapToDomain);
    }

    @Override
    public boolean existsByNameAndTeacher(String name, User teacher) {
        UserEntity teacherEntity = userJpaRepository.findById(teacher.getId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        
        return jpaRepository.existsByNameAndTeacher(name, teacherEntity);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private StudentGroupEntity mapToEntity(StudentGroup domain) {
        UserEntity teacherEntity = userJpaRepository.findById(domain.getTeacher().getId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        StudentGroupEntity entity = StudentGroupEntity.builder()
                .name(domain.getName())
                .description(domain.getDescription())
                .teacher(teacherEntity)
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }

        return entity;
    }

    private StudentGroup mapToDomain(StudentGroupEntity entity) {
        User teacher = User.builder()
                .username(entity.getTeacher().getUsername())
                .email(entity.getTeacher().getEmail())
                .firstName(entity.getTeacher().getFirstName())
                .lastName(entity.getTeacher().getLastName())
                .role(mapToDomainRole(entity.getTeacher().getRole()))
                .isActive(entity.getTeacher().getIsActive())
                .studentNumber(entity.getTeacher().getStudentNumber())
                .build();
        
        teacher.setId(entity.getTeacher().getId());
        teacher.setCreatedAt(entity.getTeacher().getCreatedAt());
        teacher.setUpdatedAt(entity.getTeacher().getUpdatedAt());

        StudentGroup domain = StudentGroup.builder()
                .name(entity.getName())
                .description(entity.getDescription())
                .teacher(teacher)
                .build();

        domain.setId(entity.getId());

        return domain;
    }

    private UserRole mapToDomainRole(UserEntityRole entityRole) {
        return switch (entityRole) {
            case STUDENT -> UserRole.STUDENT;
            case TEACHER -> UserRole.TEACHER;
            case ADMIN -> UserRole.ADMIN;
        };
    }
}