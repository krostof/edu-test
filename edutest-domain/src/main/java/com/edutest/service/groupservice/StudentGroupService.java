package com.edutest.service.groupservice;

import com.edutest.domain.group.StudentGroup;
import com.edutest.persistance.entity.user.UserEntity;
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

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class StudentGroupService {

    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public StudentGroup createStudentGroup(String name, String description, Long teacherId) {
        log.info("Creating student group: name={}, description={}, teacherId={}", name, description, teacherId);

        User teacher = userRepository.findById(teacherId)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + teacherId));

        if (studentGroupRepository.existsByNameAndTeacher(name, teacher)) {
            throw new IllegalArgumentException("Group with name '" + name + "' already exists for this teacher");
        }

        StudentGroup studentGroup = StudentGroup.builder()
                .name(name)
                .description(description)
                .teacher(teacher)
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
            if (studentGroupRepository.existsByNameAndTeacher(name, existingGroup.getTeacher())) {
                throw new IllegalArgumentException("Group with name '" + name + "' already exists for this teacher");
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
        log.info("Deleting student group with id: {}", id);

        if (!studentGroupRepository.existsById(id)) {
            throw new IllegalArgumentException("Student group not found with id: " + id);
        }

        studentGroupRepository.deleteById(id);
        log.info("Student group deleted successfully with id: {}", id);
    }

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

        User student = userMapper.toUser(studentEntity);
        group.addStudent(student);

        StudentGroup updatedGroup = studentGroupRepository.save(group);

        log.info("Student {} added to group {} successfully", studentId, groupId);
        return updatedGroup;
    }

    public StudentGroup removeStudentFromGroup(Long groupId, Long studentId) {
        log.info("Removing student {} from group {}", studentId, groupId);

        StudentGroup group = findById(groupId);
        User student = userRepository.findById(studentId)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        if (!group.containsStudent(student)) {
            throw new IllegalArgumentException("Student is not a member of this group");
        }

        group.removeStudent(student);
        StudentGroup updatedGroup = studentGroupRepository.save(group);
        
        log.info("Student {} removed from group {} successfully", studentId, groupId);
        return updatedGroup;
    }

    @Transactional(readOnly = true)
    public List<User> getGroupStudents(Long groupId) {
        log.debug("Getting students for group: {}", groupId);
        StudentGroup group = findById(groupId);
        return group.getStudents();
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
