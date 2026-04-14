package com.edutest.service.attempt;

import com.edutest.domain.test.TestAttempt;
import com.edutest.domain.user.User;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.TestAttemptJpaRepository;
import com.edutest.persistance.repository.TestRepository;
import com.edutest.persistance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TestAttemptManagementService {

    private final TestAttemptJpaRepository testAttemptRepository;
    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final AttemptRandomizationService randomizationService;

    public TestAttemptEntity startOrResumeAttempt(Long testId, Long studentId) {
        log.info("Starting or resuming attempt for test {} by student {}", testId, studentId);

        TestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found: " + testId));

        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        // Check for existing attempt
        Optional<TestAttemptEntity> existingAttempt = testAttemptRepository.findByTestIdAndStudentId(testId, studentId);

        if (existingAttempt.isPresent()) {
            TestAttemptEntity attempt = existingAttempt.get();
            if (attempt.canBeResumed()) {
                log.info("Resuming existing attempt {} for test {}", attempt.getId(), testId);
                return attempt;
            } else if (attempt.isFinished()) {
                throw new IllegalStateException("Student has already completed this test");
            }
        }

        // Validate new attempt can be started
        validateNewAttempt(test, student);

        // Create new attempt
        TestAttemptEntity newAttempt = TestAttemptEntity.builder()
                .testEntity(test)
                .student(student)
                .startedAt(LocalDateTime.now())
                .isCompleted(false)
                .currentQuestionIndex(0)
                .build();

        // Save to get ID before randomization
        newAttempt = testAttemptRepository.save(newAttempt);

        // Initialize randomization
        randomizationService.initializeRandomization(newAttempt, test);

        // Save again with randomization data
        newAttempt = testAttemptRepository.save(newAttempt);

        log.info("Created new attempt {} for test {} with randomization", newAttempt.getId(), testId);
        return newAttempt;
    }

    @Transactional(readOnly = true)
    public TestAttemptEntity getAttempt(Long attemptId) {
        return testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
    }

    @Transactional(readOnly = true)
    public TestAttemptEntity getAttemptWithTest(Long attemptId) {
        return testAttemptRepository.findByIdWithTest(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
    }

    public void updateCurrentQuestionIndex(Long attemptId, int newIndex) {
        TestAttemptEntity attempt = getAttempt(attemptId);

        if (attempt.isFinished()) {
            throw new IllegalStateException("Cannot update question index on finished attempt");
        }

        // Check if navigation is allowed
        TestEntity test = attempt.getTestEntity();
        if (!Boolean.TRUE.equals(test.getAllowNavigation())) {
            if (newIndex < attempt.getCurrentQuestionIndex()) {
                throw new IllegalStateException("Navigation to previous questions is not allowed for this test");
            }
        }

        attempt.setCurrentQuestionIndex(newIndex);
        testAttemptRepository.save(attempt);
        log.debug("Updated current question index to {} for attempt {}", newIndex, attemptId);
    }

    public boolean canAnswerQuestion(Long attemptId, int questionIndex) {
        TestAttemptEntity attempt = getAttempt(attemptId);

        if (attempt.isFinished()) {
            return false;
        }

        TestEntity test = attempt.getTestEntity();

        // If navigation is allowed, can answer any question
        if (Boolean.TRUE.equals(test.getAllowNavigation())) {
            return true;
        }

        // If navigation is not allowed, can only answer current or future questions
        return questionIndex >= attempt.getCurrentQuestionIndex();
    }

    public int getQuestionIndexForAssignment(Long attemptId, Long assignmentId) {
        TestAttemptEntity attempt = getAttemptWithTest(attemptId);
        java.util.List<Long> order = randomizationService.getAssignmentOrder(attempt);

        if (order.isEmpty()) {
            // Fallback to original order
            return attempt.getTestEntity().getAssignmentEntities().stream()
                    .filter(a -> a.getId().equals(assignmentId))
                    .findFirst()
                    .map(a -> a.getOrderNumber() - 1)
                    .orElse(-1);
        }

        return order.indexOf(assignmentId);
    }

    private void validateNewAttempt(TestEntity test, UserEntity student) {
        if (!student.isStudent()) {
            throw new IllegalArgumentException("Only students can start test attempts");
        }

        if (!test.isActive()) {
            throw new IllegalStateException("Test is not currently active");
        }

        if (!test.isAvailableForStudent(student)) {
            throw new IllegalStateException("Student is not assigned to this test");
        }
    }

    public TestAttempt toDomain(TestAttemptEntity entity) {
        TestAttempt attempt = TestAttempt.builder()
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .score(entity.getScore())
                .isCompleted(entity.getIsCompleted())
                .build();
        attempt.setId(entity.getId());
        return attempt;
    }
}
