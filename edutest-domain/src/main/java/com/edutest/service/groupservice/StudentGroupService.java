package com.edutest.service.groupservice;

import com.edutest.domain.group.StudentGroup;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.StudentGroupJpaRepository;
import com.edutest.domain.user.User;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.util.StudentGroupMapper;
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

    private final StudentGroupJpaRepository studentGroupJpaRepository;
    private final StudentGroupMapper studentGroupMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public StudentGroup createStudentGroup(String name, String description, List<Long> teacherIds) {
        log.info("Creating student group: name={}, description={}, teacherIds={}", name, description, teacherIds);

        if (studentGroupJpaRepository.existsByName(name)) {
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

        StudentGroup savedGroup = persistStudentGroup(studentGroup);
        log.info("Group created successfully with id={}", savedGroup.getId());

        return savedGroup;
    }

    @Transactional(readOnly = true)
    public StudentGroup findById(Long id) {
        log.debug("Finding student group by id: {}", id);
        return studentGroupJpaRepository.findById(id)
                .map(studentGroupMapper::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("Student group not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<StudentGroup> findByTeacher(Long teacherId) {
        log.debug("Finding student groups by teacher id: {}", teacherId);
        UserEntity teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + teacherId));

        return studentGroupJpaRepository.findByTeacher(teacher).stream()
                .map(studentGroupMapper::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<StudentGroup> findByStudent(Long studentId) {
        log.debug("Finding student group by student id: {}", studentId);
        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        return studentGroupJpaRepository.findByStudent(student)
                .map(studentGroupMapper::toDomain);
    }

    @Transactional(readOnly = true)
    public Page<StudentGroup> findAll(Pageable pageable) {
        log.debug("Finding all student groups with pagination");
        return studentGroupJpaRepository.findAll(pageable)
                .map(studentGroupMapper::toDomain);
    }

    @Transactional(readOnly = true)
    public Page<StudentGroup> findByTeacher(Long teacherId, Pageable pageable) {
        log.debug("Finding student groups by teacher id: {} with pagination", teacherId);
        UserEntity teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + teacherId));

        return studentGroupJpaRepository.findByTeacher(teacher, pageable)
                .map(studentGroupMapper::toDomain);
    }

    @Transactional(readOnly = true)
    public Page<StudentGroup> searchGroups(String searchTerm, Pageable pageable) {
        log.debug("Searching student groups with term: {}", searchTerm);
        return studentGroupJpaRepository.findByNameOrDescriptionContaining(searchTerm, pageable)
                .map(studentGroupMapper::toDomain);
    }

    public StudentGroup updateStudentGroup(Long id, String name, String description) {
        log.info("Updating student group: id={}, name={}, description={}", id, name, description);

        StudentGroup existingGroup = findById(id);

        if (name != null && !name.equals(existingGroup.getName())) {
            if (studentGroupJpaRepository.existsByName(name)) {
                throw new IllegalArgumentException("Group with name '" + name + "' already exists");
            }
            existingGroup.setName(name);
        }

        if (description != null) {
            existingGroup.setDescription(description);
        }

        StudentGroup updatedGroup = persistStudentGroup(existingGroup);
        log.info("Group updated successfully with id={}", updatedGroup.getId());

        return updatedGroup;
    }

    public void deleteStudentGroup(Long id) {
        log.info("Soft-deleting student group with id: {}", id);

        StudentGroupEntity group = studentGroupJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student group not found with id: " + id));

        // Detach students so they return to the "no group" pool (frees up the student_group_id FK),
        // but stamp deletedFromGroupId so restoreGroup can put the still-group-less ones back.
        List<UserEntity> students = userRepository.findStudentsByGroupId(id);
        for (UserEntity student : students) {
            student.setStudentGroup(null);
            student.setDeletedFromGroupId(id);
            userRepository.save(student);
        }

        // Drop assignment links so the group disappears from any test it was assigned to.
        studentGroupJpaRepository.removeGroupFromAllTests(id);

        // Soft delete: keep the row but hide it from every read via @SQLRestriction.
        group.setDeletedAt(LocalDateTime.now());
        studentGroupJpaRepository.save(group);
        log.info("Student group {} soft-deleted successfully ({} students detached)", id, students.size());
    }

    @Transactional(readOnly = true)
    public List<StudentGroup> getDeletedGroups() {
        log.debug("Listing soft-deleted student groups");
        return studentGroupJpaRepository.findAllDeleted().stream()
                .map(studentGroupMapper::toDomain)
                .toList();
    }

    @Transactional
    public StudentGroup restoreGroup(Long id) {
        log.info("Restoring student group with id: {}", id);
        StudentGroup group = studentGroupJpaRepository.findDeletedById(id)
                .map(studentGroupMapper::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("Deleted student group not found with id: " + id));

        studentGroupJpaRepository.restoreById(id);

        // Re-attach students detached on deletion, but skip anyone who has since joined another
        // group (their current membership wins). Clear the marker either way so it can't re-fire.
        // Test-assignment links (test_groups) are intentionally NOT restored: the teacher may have
        // restructured the test while the group was gone — re-injecting it silently would surprise.
        StudentGroupEntity groupEntity = studentGroupJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Group not visible after restore: " + id));
        List<UserEntity> formerMembers = userRepository.findByDeletedFromGroupId(id);
        int reattached = 0;
        for (UserEntity student : formerMembers) {
            if (student.getStudentGroup() == null) {
                student.setStudentGroup(groupEntity);
                reattached++;
            }
            student.setDeletedFromGroupId(null);
            userRepository.save(student);
        }
        log.info("Student group {} restored: {} of {} former students re-attached",
                id, reattached, formerMembers.size());

        // Reload so the response reflects the re-attached members (count, list).
        return studentGroupJpaRepository.findById(id)
                .map(studentGroupMapper::toDomain)
                .orElse(group);
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
        StudentGroup updatedGroup = persistStudentGroup(group);

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
        StudentGroup updatedGroup = persistStudentGroup(group);

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

    private StudentGroup persistStudentGroup(StudentGroup studentGroup) {
        StudentGroupEntity entity = studentGroup.getId() != null
                ? studentGroupJpaRepository.findById(studentGroup.getId()).orElseGet(StudentGroupEntity::new)
                : new StudentGroupEntity();

        entity.setName(studentGroup.getName());
        entity.setDescription(studentGroup.getDescription());
        if (studentGroup.getId() != null) {
            entity.setId(studentGroup.getId());
        }

        List<UserEntity> teacherEntities = new ArrayList<>();
        for (User teacher : studentGroup.getTeachers()) {
            UserEntity teacherEntity = userRepository.findById(teacher.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacher.getId()));
            teacherEntities.add(teacherEntity);
        }
        entity.getTeachers().clear();
        entity.getTeachers().addAll(teacherEntities);

        StudentGroupEntity savedEntity = studentGroupJpaRepository.save(entity);
        return studentGroupMapper.toDomain(savedEntity);
    }
}
