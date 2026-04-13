package com.edutest.service.answer;

import com.edutest.dto.TestSubmissionResultDto;
import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.assigment.AssignmentType;
import com.edutest.persistance.entity.assigment.common.AssignmentAnswerEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestSubmissionService {

    private final TestAttemptJpaRepository testAttemptRepository;
    private final AssignmentAnswerJpaRepository answerRepository;
    private final CodeSubmissionJpaRepository codeSubmissionRepository;
    private final AssignmentJpaRepository assignmentRepository;

    @Transactional
    public TestSubmissionResultDto submitTestAttempt(Long testId, Long attemptId, Long studentId) {
        TestAttemptEntity attempt = testAttemptRepository.findByIdWithTest(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Test attempt not found"));

        if (!attempt.getTestEntity().getId().equals(testId)) {
            throw new IllegalArgumentException("Test attempt does not belong to this test");
        }

        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("You do not have access to this test attempt");
        }

        if (attempt.isFinished()) {
            throw new IllegalStateException("Test attempt is already completed");
        }

        autoGradeAllAnswers(attemptId);

        float totalScore = calculateTotalScore(attemptId);
        Float maxPossibleScore = assignmentRepository.sumPointsByTestId(testId);

        attempt.finish(totalScore);
        testAttemptRepository.save(attempt);

        int gradedCount = countGradedAnswers(attemptId);
        int pendingGradingCount = countPendingGrading(attemptId, testId);

        return TestSubmissionResultDto.create(
                attemptId,
                testId,
                attempt.getFinishedAt(),
                totalScore,
                maxPossibleScore != null ? maxPossibleScore.floatValue() : 0f,
                gradedCount,
                pendingGradingCount
        );
    }

    private void autoGradeAllAnswers(Long attemptId) {
        List<AssignmentAnswerEntity> answers = answerRepository.findByTestAttemptId(attemptId);

        for (AssignmentAnswerEntity answer : answers) {
            AssignmentType type = answer.getAssignmentEntity().getType();

            if (type.isAutoGradeable() && !Boolean.TRUE.equals(answer.getIsGraded())) {
                if (type == AssignmentType.SINGLE_CHOICE || type == AssignmentType.MULTIPLE_CHOICE) {
                    answer.autoGrade();
                    answerRepository.save(answer);
                }
            }
        }
    }

    private float calculateTotalScore(Long attemptId) {
        float total = 0f;

        Float answerScores = answerRepository.sumScoresByTestAttemptId(attemptId);
        if (answerScores != null) {
            total += answerScores;
        }

        Float codeScores = codeSubmissionRepository.sumScoresByTestAttemptId(attemptId);
        if (codeScores != null) {
            total += codeScores;
        }

        return total;
    }

    private int countGradedAnswers(Long attemptId) {
        int count = 0;

        long gradedNonCode = answerRepository.countGradedByTestAttemptId(attemptId);
        count += (int) gradedNonCode;

        List<CodeSubmissionEntity> codeSubmissions = codeSubmissionRepository.findByTestAttemptId(attemptId);
        count += (int) codeSubmissions.stream()
                .filter(s -> s.getTotalScore() != null)
                .count();

        return count;
    }

    private int countPendingGrading(Long attemptId, Long testId) {
        List<AssignmentEntity> allAssignments = assignmentRepository.findByTestEntityIdOrderByOrderNumber(testId);

        int totalAssignments = allAssignments.size();
        int answeredAndGraded = countGradedAnswers(attemptId);

        long ungradedNonCode = answerRepository.countUngradedByTestAttemptId(attemptId);

        return (int) ungradedNonCode;
    }

    @Transactional(readOnly = true)
    public boolean isAttemptCompleted(Long attemptId) {
        return testAttemptRepository.findById(attemptId)
                .map(TestAttemptEntity::isFinished)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canAccessResults(Long attemptId, Long studentId) {
        return testAttemptRepository.findById(attemptId)
                .map(attempt ->
                        attempt.getStudent().getId().equals(studentId) && attempt.isFinished())
                .orElse(false);
    }
}
