package com.edutest.service.testservice;

import com.edutest.domain.test.Test;
import com.edutest.domain.test.TestAttempt;
import com.edutest.domain.user.User;
import com.edutest.domain.group.StudentGroup;
import com.edutest.persistance.repository.TestRepository;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.persistance.repository.StudentGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TestService {

    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final StudentGroupRepository studentGroupRepository;

    public Test createTest(String title, String description, LocalDateTime startDate, 
                          LocalDateTime endDate, Integer timeLimit, Boolean allowNavigation, 
                          Boolean randomizeOrder, Long createdById) {
        
        log.info("Creating test: title={}, createdById={}", title, createdById);

        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found with id: " + createdById));

        if (!creator.isTeacher() && !creator.isAdmin()) {
            throw new IllegalArgumentException("Only teachers and admins can create tests");
        }

        validateTestDates(startDate, endDate);

        if (testRepository.existsByTitleAndCreatedBy(title, creator)) {
            throw new IllegalArgumentException("Test with title '" + title + "' already exists for this user");
        }

        Test test = Test.builder()
                .title(title)
                .description(description)
                .startDate(startDate)
                .endDate(endDate)
                .timeLimit(timeLimit)
                .allowNavigation(allowNavigation != null ? allowNavigation : true)
                .randomizeOrder(randomizeOrder != null ? randomizeOrder : false)
                .createdBy(creator)
                .build();

        Test savedTest = testRepository.save(test);
        log.info("Test created successfully with id={}", savedTest.getId());

        return savedTest;
    }

    @Transactional(readOnly = true)
    public Test findById(Long id) {
        log.debug("Finding test by id: {}", id);
        return testRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Test> findByCreatedBy(Long createdById) {
        log.debug("Finding tests by creator id: {}", createdById);
        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found with id: " + createdById));
        
        return testRepository.findByCreatedBy(creator);
    }

    @Transactional(readOnly = true)
    public Page<Test> findByCreatedBy(Long createdById, Pageable pageable) {
        log.debug("Finding tests by creator id: {} with pagination", createdById);
        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found with id: " + createdById));
        
        return testRepository.findByCreatedBy(creator, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Test> findAll(Pageable pageable) {
        log.debug("Finding all tests with pagination");
        return testRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Test> findActiveTests() {
        log.debug("Finding active tests");
        return testRepository.findActiveTests();
    }

    @Transactional(readOnly = true)
    public List<Test> findUpcomingTests() {
        log.debug("Finding upcoming tests");
        return testRepository.findUpcomingTests();
    }

    @Transactional(readOnly = true)
    public List<Test> findExpiredTests() {
        log.debug("Finding expired tests");
        return testRepository.findExpiredTests();
    }

    @Transactional(readOnly = true)
    public List<Test> findAvailableTestsForStudent(Long studentId) {
        log.debug("Finding available tests for student: {}", studentId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        if (!student.isStudent()) {
            throw new IllegalArgumentException("User with id " + studentId + " is not a student");
        }

        return testRepository.findAvailableTestsForStudent(student);
    }

    @Transactional(readOnly = true)
    public List<Test> findTestsByGroup(Long groupId) {
        log.debug("Finding tests by group: {}", groupId);
        StudentGroup group = studentGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));
        
        return testRepository.findTestsByGroup(group);
    }

    @Transactional(readOnly = true)
    public Page<Test> searchTests(String searchTerm, Pageable pageable) {
        log.debug("Searching tests with term: {}", searchTerm);
        return testRepository.findByTitleOrDescriptionContaining(searchTerm, pageable);
    }

    public Test updateTest(Long id, String title, String description, LocalDateTime startDate, 
                          LocalDateTime endDate, Integer timeLimit, Boolean allowNavigation, 
                          Boolean randomizeOrder) {
        
        log.info("Updating test: id={}", id);

        Test existingTest = findById(id);

        if (title != null && !title.equals(existingTest.getTitle())) {
            if (testRepository.existsByTitleAndCreatedBy(title, existingTest.getCreatedBy())) {
                throw new IllegalArgumentException("Test with title '" + title + "' already exists for this user");
            }
            existingTest.setTitle(title);
        }

        if (description != null) {
            existingTest.setDescription(description);
        }

        if (startDate != null && endDate != null) {
            validateTestDates(startDate, endDate);
            existingTest.setStartDate(startDate);
            existingTest.setEndDate(endDate);
        } else if (startDate != null) {
            validateTestDates(startDate, existingTest.getEndDate());
            existingTest.setStartDate(startDate);
        } else if (endDate != null) {
            validateTestDates(existingTest.getStartDate(), endDate);
            existingTest.setEndDate(endDate);
        }

        if (timeLimit != null) {
            existingTest.setTimeLimit(timeLimit);
        }

        if (allowNavigation != null) {
            existingTest.setAllowNavigation(allowNavigation);
        }

        if (randomizeOrder != null) {
            existingTest.setRandomizeOrder(randomizeOrder);
        }

        Test updatedTest = testRepository.save(existingTest);
        log.info("Test updated successfully with id={}", updatedTest.getId());

        return updatedTest;
    }

    public void deleteTest(Long id) {
        log.info("Deleting test with id: {}", id);

        Test test = findById(id);

        if (!test.getAttempts().isEmpty()) {
            throw new IllegalStateException("Cannot delete test with existing attempts");
        }

        testRepository.deleteById(id);
        log.info("Test deleted successfully with id: {}", id);
    }

    public Test assignGroupToTest(Long testId, Long groupId) {
        log.info("Assigning group {} to test {}", groupId, testId);

        Test test = findById(testId);
        StudentGroup group = studentGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));

        if (test.getAssignedGroups().contains(group)) {
            throw new IllegalArgumentException("Group is already assigned to this test");
        }

        test.addGroup(group);
        Test updatedTest = testRepository.save(test);
        
        log.info("Group {} assigned to test {} successfully", groupId, testId);
        return updatedTest;
    }

    public Test removeGroupFromTest(Long testId, Long groupId) {
        log.info("Removing group {} from test {}", groupId, testId);

        Test test = findById(testId);
        StudentGroup group = studentGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));

        if (!test.getAssignedGroups().contains(group)) {
            throw new IllegalArgumentException("Group is not assigned to this test");
        }

        test.removeGroup(group);
        Test updatedTest = testRepository.save(test);
        
        log.info("Group {} removed from test {} successfully", groupId, testId);
        return updatedTest;
    }

    @Transactional(readOnly = true)
    public List<StudentGroup> getTestGroups(Long testId) {
        log.debug("Getting groups for test: {}", testId);
        Test test = findById(testId);
        return test.getAssignedGroups();
    }

    @Transactional(readOnly = true)
    public boolean isTestAvailableForStudent(Long testId, Long studentId) {
        log.debug("Checking if test {} is available for student {}", testId, studentId);
        
        Test test = findById(testId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        return test.isAvailableForStudent(student);
    }

    @Transactional(readOnly = true)
    public boolean hasStudentStartedAttempt(Long testId, Long studentId) {
        log.debug("Checking if student {} has started attempt for test {}", studentId, testId);
        
        Test test = findById(testId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        return test.hasStudentStartedAttempt(student);
    }

    @Transactional(readOnly = true)
    public TestAttempt getStudentAttempt(Long testId, Long studentId) {
        log.debug("Getting attempt for student {} and test {}", studentId, testId);
        
        Test test = findById(testId);
        User student = userRepository.findById(studentId)
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
        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found with id: " + createdById));
        
        return testRepository.countByCreatedBy(creator);
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