package com.edutest.service.groupservice;

import com.edutest.domain.group.StudentGroup;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.StudentGroupJpaRepository;
import com.edutest.persistance.repository.StudentGroupRepository;
import com.edutest.domain.user.User;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edutest.persistance.entity.group.StudentGroupEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class StudentGroupService {

    private final StudentGroupRepository studentGroupRepository;
    private final StudentGroupJpaRepository studentGroupJpaRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public StudentGroup createStudentGroup(String name, String description, List<Long> teacherIds) {
        log.info("Creating student group: name={}, description={}, teacherIds={}", name, description, teacherIds);

        if (studentGroupRepository.existsByName(name)) {
            throw new IllegalArgumentException("Group with name '" + name + "' already exists");
        }

        List<User> teachers = new ArrayList<>();
        if (teacherIds != null) {
            for (Long teacherId : teacherIds) {
                User teacher = userRepository.findById(teacherId)
                        .map(userMapper::toUser)
                        .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + teacherId));

                if (!teacher.isTeacher()) {
                    throw new IllegalArgumentException("User with id " + teacherId + " is not a teacher");
                }
                teachers.add(teacher);
            }
        }

        StudentGroup studentGroup = StudentGroup.builder()
                .name(name)
                .description(description)
                .teachers(teachers)
                .build();

        StudentGroup savedGroup = studentGroupRepository.save(studentGroup);
        log.info("Group created successfully with id={}", savedGroup.getId());

        return savedGroup;
    }

    @Transactional(readOnly = true)
    public StudentGroup findById(Long id) {
        log.debug("Finding student group by id: {}", id);
        return studentGroupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student group not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<StudentGroup> findByTeacher(Long teacherId) {
        log.debug("Finding student groups by teacher id: {}", teacherId);
        User teacher = userRepository.findById(teacherId)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + teacherId));

        return studentGroupRepository.findByTeacher(teacher);
    }

    @Transactional(readOnly = true)
    public Optional<StudentGroup> findByStudent(Long studentId) {
        log.debug("Finding student group by student id: {}", studentId);
        User student = userRepository.findById(studentId)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        return studentGroupRepository.findByStudent(student);
    }

    @Transactional(readOnly = true)
    public Page<StudentGroup> findAll(Pageable pageable) {
        log.debug("Finding all student groups with pagination");
        return studentGroupRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<StudentGroup> findByTeacher(Long teacherId, Pageable pageable) {
        log.debug("Finding student groups by teacher id: {} with pagination", teacherId);
        User teacher = userRepository.findById(teacherId)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + teacherId));

        return studentGroupRepository.findByTeacher(teacher, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StudentGroup> searchGroups(String searchTerm, Pageable pageable) {
        log.debug("Searching student groups with term: {}", searchTerm);
        return studentGroupRepository.findByNameOrDescriptionContaining(searchTerm, pageable);
    }

    public StudentGroup updateStudentGroup(Long id, String name, String description) {
        log.info("Updating student group: id={}, name={}, description={}", id, name, description);

        StudentGroup existingGroup = findById(id);

        if (name != null && !name.equals(existingGroup.getName())) {
            if (studentGroupRepository.existsByName(name)) {
                throw new IllegalArgumentException("Group with name '" + name + "' already exists");
            }
            existingGroup.setName(name);
        }

        if (description != null) {
            existingGroup.setDescription(description);
        }

        StudentGroup updatedGroup = studentGroupRepository.save(existingGroup);
        log.info("Group updated successfully with id={}", updatedGroup.getId());

        return updatedGroup;
    }

    public void deleteStudentGroup(Long id) {
        log.info("Soft-deleting student group with id: {}", id);

        StudentGroupEntity group = studentGroupJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student group not found with id: " + id));

        // Detach students so they return to the "no group" pool (frees up the student_group_id FK).
        List<UserEntity> students = userRepository.findStudentsByGroupId(id);
        for (UserEntity student : students) {
            student.setStudentGroup(null);
            userRepository.save(student);
        }

        // Drop assignment links so the group disappears from any test it was assigned to.
        studentGroupJpaRepository.removeGroupFromAllTests(id);

        // Soft delete: keep the row but hide it from every read via @SQLRestriction.
        group.setDeletedAt(LocalDateTime.now());
        studentGroupJpaRepository.save(group);
        log.info("Student group {} soft-deleted successfully ({} students detached)", id, students.size());
    }

    // Teacher management
    public StudentGroup addTeacherToGroup(Long groupId, Long teacherId) {
        log.info("Adding teacher {} to group {}", teacherId, groupId);

        StudentGroup group = findById(groupId);
        User teacher = userRepository.findById(teacherId)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + teacherId));

        if (!teacher.isTeacher()) {
            throw new IllegalArgumentException("User with id " + teacherId + " is not a teacher");
        }

        if (group.hasTeacher(teacher)) {
            throw new IllegalArgumentException("Teacher is already assigned to this group");
        }

        group.addTeacher(teacher);
        StudentGroup updatedGroup = studentGroupRepository.save(group);

        log.info("Teacher {} added to group {} successfully", teacherId, groupId);
        return updatedGroup;
    }

    public StudentGroup removeTeacherFromGroup(Long groupId, Long teacherId) {
        log.info("Removing teacher {} from group {}", teacherId, groupId);

        StudentGroup group = findById(groupId);
        User teacher = userRepository.findById(teacherId)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + teacherId));

        if (!group.hasTeacher(teacher)) {
            throw new IllegalArgumentException("Teacher is not assigned to this group");
        }

        group.removeTeacher(teacher);
        StudentGroup updatedGroup = studentGroupRepository.save(group);

        log.info("Teacher {} removed from group {} successfully", teacherId, groupId);
        return updatedGroup;
    }

    // Student management
    public StudentGroup addStudentToGroup(Long groupId, Long studentId) {
        log.info("Adding student {} to group {}", studentId, groupId);

        StudentGroup group = findById(groupId);
        UserEntity studentEntity = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        if (!studentEntity.isStudent()) {
            throw new IllegalArgumentException("User with id " + studentId + " is not a student");
        }

        if (studentEntity.getStudentGroup() != null) {
            if (studentEntity.getStudentGroup().getId().equals(groupId)) {
                throw new IllegalArgumentException("Student is already in this group");
            }
            throw new IllegalStateException("Student is already in group: " +
                studentEntity.getStudentGroup().getName());
        }

        // Update the owning side of the relationship (UserEntity.studentGroup)
        studentEntity.setStudentGroup(
            studentGroupJpaRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId))
        );
        userRepository.save(studentEntity);

        log.info("Student {} added to group {} successfully", studentId, groupId);
        return findById(groupId); // Reload to get updated students list
    }

    public StudentGroup addStudentsToGroup(Long groupId, List<Long> studentIds) {
        log.info("Adding {} students to group {}", studentIds.size(), groupId);

        var groupEntity = studentGroupJpaRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));

        for (Long studentId : studentIds) {
            UserEntity studentEntity = userRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

            if (!studentEntity.isStudent()) {
                throw new IllegalArgumentException("User with id " + studentId + " is not a student");
            }

            if (studentEntity.getStudentGroup() != null) {
                if (studentEntity.getStudentGroup().getId().equals(groupId)) {
                    log.warn("Student {} is already in this group, skipping", studentId);
                    continue;
                }
                throw new IllegalStateException("Student " + studentId + " is already in group: " +
                    studentEntity.getStudentGroup().getName());
            }

            // Update the owning side of the relationship
            studentEntity.setStudentGroup(groupEntity);
            userRepository.save(studentEntity);
        }

        log.info("{} students added to group {} successfully", studentIds.size(), groupId);
        return findById(groupId); // Reload to get updated students list
    }

    public StudentGroup removeStudentFromGroup(Long groupId, Long studentId) {
        log.info("Removing student {} from group {}", studentId, groupId);

        UserEntity studentEntity = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        if (studentEntity.getStudentGroup() == null || !studentEntity.getStudentGroup().getId().equals(groupId)) {
            throw new IllegalArgumentException("Student is not a member of this group");
        }

        // Update the owning side of the relationship
        studentEntity.setStudentGroup(null);
        userRepository.save(studentEntity);

        log.info("Student {} removed from group {} successfully", studentId, groupId);
        return findById(groupId); // Reload to get updated students list
    }

    @Transactional(readOnly = true)
    public List<User> getGroupStudents(Long groupId) {
        log.debug("Getting students for group: {}", groupId);
        StudentGroup group = findById(groupId);
        return group.getStudents();
    }

    @Transactional(readOnly = true)
    public List<User> getGroupTeachers(Long groupId) {
        log.debug("Getting teachers for group: {}", groupId);
        StudentGroup group = findById(groupId);
        return group.getTeachers();
    }

    @Transactional(readOnly = true)
    public int getGroupStudentCount(Long groupId) {
        log.debug("Getting student count for group: {}", groupId);
        StudentGroup group = findById(groupId);
        return group.getStudentCount();
    }

    @Transactional(readOnly = true)
    public boolean isStudentInGroup(Long groupId, Long studentId) {
        log.debug("Checking if student {} is in group {}", studentId, groupId);

        StudentGroup group = findById(groupId);
        User student = userRepository.findById(studentId)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        return group.containsStudent(student);
    }
}
