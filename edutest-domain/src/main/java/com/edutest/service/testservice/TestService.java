package com.edutest.service.testservice;

import com.edutest.domain.test.Test;
import com.edutest.domain.test.TestAttempt;
import com.edutest.domain.user.User;
import com.edutest.domain.group.StudentGroup;
import com.edutest.persistance.entity.group.StudentGroupEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.StudentGroupJpaRepository;
import com.edutest.persistance.repository.TestRepository;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.util.TestMapper;
import com.edutest.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TestService {

    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final StudentGroupJpaRepository studentGroupJpaRepository;
    private final UserMapper userMapper;
    private final TestMapper testMapper;

    public Test createTest(String title, String description, LocalDateTime startDate,
                          LocalDateTime endDate, Integer timeLimit, Boolean allowNavigation,
                          Boolean randomizeOrder, Long createdById) {

        log.info("Creating test: title={}, createdById={}", title, createdById);

        UserEntity creatorEntity = userRepository.findById(createdById)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found with id: " + createdById));

        User creator = userMapper.toUser(creatorEntity);
        if (!creator.isTeacher()) {
            throw new IllegalArgumentException("Only teachers can create tests");
        }

        validateTestDates(startDate, endDate);

        if (testRepository.existsByTitleAndCreatedBy(title, creatorEntity)) {
            throw new IllegalArgumentException("Test with title '" + title + "' already exists for this user");
        }

        TestEntity testEntity = TestEntity.builder()
                .title(title)
                .description(description)
                .startDate(startDate)
                .endDate(endDate)
                .timeLimit(timeLimit)
                .allowNavigation(allowNavigation != null ? allowNavigation : true)
                .randomizeOrder(randomizeOrder != null ? randomizeOrder : false)
                .createdBy(creatorEntity)
                .build();

        TestEntity savedEntity = testRepository.save(testEntity);
        log.info("Test created successfully with id={}", savedEntity.getId());

        return testMapper.toDomain(savedEntity);
    }

    @Transactional(readOnly = true)
    public Test findById(Long id) {
        log.debug("Finding test by id: {}", id);
        return testRepository.findById(id)
                .map(testMapper::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("Test not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Test> findByCreatedBy(Long createdById) {
        log.debug("Finding tests by creator id: {}", createdById);
        UserEntity creatorEntity = userRepository.findById(createdById)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found with id: " + createdById));

        return testRepository.findByCreatedBy(creatorEntity).stream()
                .map(testMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<Test> findByCreatedBy(Long createdById, Pageable pageable) {
        log.debug("Finding tests by creator id: {} with pagination", createdById);
        UserEntity creatorEntity = userRepository.findById(createdById)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found with id: " + createdById));

        return testRepository.findByCreatedBy(creatorEntity, pageable)
                .map(testMapper::toDomain);
    }

    @Transactional(readOnly = true)
    public Page<TestEntity> findAll(Pageable pageable) {
        log.debug("Finding all tests with pagination");
        return testRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Test> findActiveTests() {
        log.debug("Finding active tests");
        return testRepository.findActiveTests().stream()
                .map(testMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Test> findUpcomingTests() {
        log.debug("Finding upcoming tests");
        return testRepository.findUpcomingTests().stream()
                .map(testMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Test> findExpiredTests() {
        log.debug("Finding expired tests");
        return testRepository.findExpiredTests().stream()
                .map(testMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Test> findAvailableTestsForStudent(Long studentId) {
        log.debug("Finding available tests for student: {}", studentId);
        UserEntity studentEntity = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        User student = userMapper.toUser(studentEntity);
        if (!student.isStudent()) {
            throw new IllegalArgumentException("User with id " + studentId + " is not a student");
        }

        return testRepository.findAvailableTestsForStudent(studentId).stream()
                .map(testMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Test> findTestsByGroup(Long groupId) {
        log.debug("Finding tests by group: {}", groupId);
        if (!studentGroupJpaRepository.existsById(groupId)) {
            throw new IllegalArgumentException("Group not found with id: " + groupId);
        }

        return testRepository.findTestsByGroupId(groupId).stream()
                .map(testMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<Test> searchTests(String searchTerm, Pageable pageable) {
        log.debug("Searching tests with term: {}", searchTerm);
        return testRepository.findByTitleOrDescriptionContaining(searchTerm, pageable)
                .map(testMapper::toDomain);
    }

    public Test updateTest(Long id, String title, String description, LocalDateTime startDate,
                          LocalDateTime endDate, Integer timeLimit, Boolean allowNavigation,
                          Boolean randomizeOrder) {

        log.info("Updating test: id={}", id);

        TestEntity existingEntity = testRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test not found with id: " + id));

        if (title != null && !title.equals(existingEntity.getTitle())) {
            if (testRepository.existsByTitleAndCreatedBy(title, existingEntity.getCreatedBy())) {
                throw new IllegalArgumentException("Test with title '" + title + "' already exists for this user");
            }
            existingEntity.setTitle(title);
        }

        if (description != null) {
            existingEntity.setDescription(description);
        }

        if (startDate != null && endDate != null) {
            validateTestDates(startDate, endDate);
            existingEntity.setStartDate(startDate);
            existingEntity.setEndDate(endDate);
        } else if (startDate != null) {
            validateTestDates(startDate, existingEntity.getEndDate());
            existingEntity.setStartDate(startDate);
        } else if (endDate != null) {
            validateTestDates(existingEntity.getStartDate(), endDate);
            existingEntity.setEndDate(endDate);
        }

        if (timeLimit != null) {
            existingEntity.setTimeLimit(timeLimit);
        }

        if (allowNavigation != null) {
            existingEntity.setAllowNavigation(allowNavigation);
        }

        if (randomizeOrder != null) {
            existingEntity.setRandomizeOrder(randomizeOrder);
        }

        TestEntity updatedEntity = testRepository.save(existingEntity);
        log.info("Test updated successfully with id={}", updatedEntity.getId());

        return testMapper.toDomain(updatedEntity);
    }

    public void deleteTest(Long id) {
        log.info("Deleting test with id: {}", id);

        TestEntity testEntity = testRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test not found with id: " + id));

        if (!testEntity.getAttempts().isEmpty()) {
            throw new IllegalStateException("Cannot delete test with existing attempts");
        }

        testRepository.deleteById(id);
        log.info("Test deleted successfully with id: {}", id);
    }

    public Test assignGroupToTest(Long testId, Long groupId) {
        log.info("Assigning group {} to test {}", groupId, testId);

        TestEntity testEntity = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found with id: " + testId));
        StudentGroupEntity groupEntity = studentGroupJpaRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));

        if (testEntity.getAssignedGroups().contains(groupEntity)) {
            throw new IllegalArgumentException("Group is already assigned to this test");
        }

        testEntity.addGroup(groupEntity);
        TestEntity updatedEntity = testRepository.save(testEntity);

        log.info("Group {} assigned to test {} successfully", groupId, testId);
        return testMapper.toDomain(updatedEntity);
    }

    public Test removeGroupFromTest(Long testId, Long groupId) {
        log.info("Removing group {} from test {}", groupId, testId);

        TestEntity testEntity = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found with id: " + testId));
        StudentGroupEntity groupEntity = studentGroupJpaRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));

        if (!testEntity.getAssignedGroups().contains(groupEntity)) {
            throw new IllegalArgumentException("Group is not assigned to this test");
        }

        testEntity.removeGroup(groupEntity);
        TestEntity updatedEntity = testRepository.save(testEntity);

        log.info("Group {} removed from test {} successfully", groupId, testId);
        return testMapper.toDomain(updatedEntity);
    }

    @Transactional(readOnly = true)
    public List<StudentGroup> getTestGroups(Long testId) {
        log.debug("Getting groups for test: {}", testId);
        TestEntity testEntity = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found with id: " + testId));

        return testEntity.getAssignedGroups().stream()
                .map(this::toStudentGroup)
                .collect(Collectors.toList());
    }

    private StudentGroup toStudentGroup(StudentGroupEntity entity) {
        // Force load students to get correct count (they are LAZY loaded)
        List<User> students = new ArrayList<>();
        if (entity.getStudents() != null) {
            for (UserEntity studentEntity : entity.getStudents()) {
                User student = User.builder()
                        .username(studentEntity.getUsername())
                        .email(studentEntity.getEmail())
                        .firstName(studentEntity.getFirstName())
                        .lastName(studentEntity.getLastName())
                        .build();
                student.setId(studentEntity.getId());
                students.add(student);
            }
        }

        StudentGroup group = StudentGroup.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .students(students)
                .build();

        return group;
    }

    @Transactional(readOnly = true)
    public boolean isTestAvailableForStudent(Long testId, Long studentId) {
        log.debug("Checking if test {} is available for student {}", testId, studentId);
        
        Test test = findById(testId);
        User student = userRepository.findById(studentId)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        return test.isAvailableForStudent(student);
    }

    @Transactional(readOnly = true)
    public boolean hasStudentStartedAttempt(Long testId, Long studentId) {
        log.debug("Checking if student {} has started attempt for test {}", studentId, testId);
        
        Test test = findById(testId);
        User student = userRepository.findById(studentId)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        return test.hasStudentStartedAttempt(student);
    }

    @Transactional(readOnly = true)
    public TestAttempt getStudentAttempt(Long testId, Long studentId) {
        log.debug("Getting attempt for student {} and test {}", studentId, testId);
        
        Test test = findById(testId);
        User student = userRepository.findById(studentId)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        return test.getStudentAttempt(student);
    }

    @Transactional(readOnly = true)
    public int getTestTotalPoints(Long testId) {
        log.debug("Getting total points for test: {}", testId);
        Test test = findById(testId);
        return test.getTotalPoints();
    }

    @Transactional(readOnly = true)
    public int getTestAssignmentCount(Long testId) {
        log.debug("Getting assignment count for test: {}", testId);
        Test test = findById(testId);
        return test.getAssignmentCount();
    }

    @Transactional(readOnly = true)
    public long countTests() {
        return testRepository.count();
    }

    @Transactional(readOnly = true)
    public long countTestsByCreator(Long createdById) {
        UserEntity creatorEntity = userRepository.findById(createdById)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found with id: " + createdById));

        return testRepository.countByCreatedBy(creatorEntity);
    }

    private void validateTestDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        if (endDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("End date cannot be in the past");
        }
    }
}