package com.edutest.service.teacher;

import com.edutest.dto.AnswerReviewDto;
import com.edutest.dto.ChoiceOptionDto;
import com.edutest.dto.GradeAnswerRequestDto;
import com.edutest.dto.TestCaseResultDto;
import com.edutest.event.AnswerGradedEvent;
import com.edutest.persistance.entity.test.TestCaseResultEntity;
import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.assigment.AssignmentType;
import com.edutest.persistance.entity.assigment.common.AssignmentAnswerEntity;
import com.edutest.persistance.entity.assigment.common.ChoiceOptionEntity;
import com.edutest.persistance.entity.assigment.multiplechoice.MultipleChoiceAnswerEntity;
import com.edutest.persistance.entity.assigment.multiplechoice.MultipleChoiceAssignmentEntity;
import com.edutest.persistance.entity.assigment.openquestion.OpenQuestionAnswerEntity;
import com.edutest.persistance.entity.assigment.openquestion.OpenQuestionAssignmentEntityEntity;
import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAnswerEntity;
import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAssignmentEntityEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles teacher's manual grading of student answers.
 *
 * Despite living in the {@code teacher} package, this covers all assignment types
 * that need (or admit) manual review:
 *  - {@code OPEN_QUESTION} — typically the only type that requires manual grading
 *  - {@code CODING} — auto-scored at submit, but teacher can override the score and
 *    add written feedback (e.g., "good logic, but ignored edge case for empty input")
 *  - {@code SINGLE_CHOICE} / {@code MULTIPLE_CHOICE} — auto-graded; manual override
 *    rare but supported (e.g., to compensate for a bad question)
 *
 * Auto-grading at test submission lives in {@code TestSubmissionService.autoGradeAllAnswers}.
 *
 * Publishes {@link com.edutest.event.AnswerGradedEvent} after every successful grade.
 * This is a primitive fact ("this answer was graded") — derived events like
 * "attempt fully graded" come from {@code AttemptGradingStateTracker}.
 */
@Service
@RequiredArgsConstructor
public class ManualGradingService {

    private final TestAttemptJpaRepository testAttemptRepository;
    private final AssignmentJpaRepository assignmentRepository;
    private final AssignmentAnswerJpaRepository answerRepository;
    private final CodeSubmissionJpaRepository codeSubmissionRepository;
    private final ChoiceOptionJpaRepository choiceOptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public AnswerReviewDto getAnswerForReview(Long testId, Long attemptId, Long assignmentId) {
        TestAttemptEntity attempt = testAttemptRepository.findByIdWithTestAndStudent(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Test attempt not found"));

        if (!attempt.getTestEntity().getId().equals(testId)) {
            throw new IllegalArgumentException("Test attempt does not belong to this test");
        }

        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        if (!assignment.getTestEntity().getId().equals(testId)) {
            throw new IllegalArgumentException("Assignment does not belong to this test");
        }

        UserEntity student = attempt.getStudent();
        AssignmentType type = assignment.getType();

        if (type == AssignmentType.CODING) {
            CodeSubmissionEntity submission = codeSubmissionRepository
                    .findByTestAttemptIdAndAssignmentId(attemptId, assignmentId)
                    .orElse(null);
            return buildCodingReviewDto(assignment, attempt, student, submission);
        } else {
            AssignmentAnswerEntity answer = answerRepository
                    .findByTestAttemptIdAndAssignmentId(attemptId, assignmentId)
                    .orElse(null);
            return buildAnswerReviewDto(assignment, attempt, student, answer);
        }
    }

    @Transactional
    public AnswerReviewDto gradeAnswer(Long testId, Long attemptId, Long assignmentId, GradeAnswerRequestDto request) {
        TestAttemptEntity attempt = testAttemptRepository.findByIdWithTestAndStudent(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Test attempt not found"));

        if (!attempt.getTestEntity().getId().equals(testId)) {
            throw new IllegalArgumentException("Test attempt does not belong to this test");
        }

        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        if (!assignment.getTestEntity().getId().equals(testId)) {
            throw new IllegalArgumentException("Assignment does not belong to this test");
        }

        // Validate score range
        if (request.getScore() == null) {
            throw new IllegalArgumentException("Score is required");
        }
        if (request.getScore() < 0) {
            throw new IllegalArgumentException("Score cannot be negative");
        }
        if (request.getScore() > assignment.getPoints()) {
            throw new IllegalArgumentException("Score cannot exceed max points (" + assignment.getPoints() + ")");
        }

        AssignmentType type = assignment.getType();

        if (type == AssignmentType.CODING) {
            CodeSubmissionEntity submission = codeSubmissionRepository
                    .findByTestAttemptIdAndAssignmentId(attemptId, assignmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Code submission not found"));

            submission.setTotalScore(request.getScore());
            submission.setTeacherFeedback(request.getFeedback());
            codeSubmissionRepository.save(submission);

            recalculateAttemptScore(attempt);

            publishAnswerGraded(attempt, assignment);

            return buildCodingReviewDto(assignment, attempt, attempt.getStudent(), submission);
        } else {
            AssignmentAnswerEntity answer = answerRepository
                    .findByTestAttemptIdAndAssignmentId(attemptId, assignmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Answer not found"));

            answer.grade(request.getScore(), request.getFeedback());
            answerRepository.save(answer);

            recalculateAttemptScore(attempt);

            publishAnswerGraded(attempt, assignment);

            return buildAnswerReviewDto(assignment, attempt, attempt.getStudent(), answer);
        }
    }

    /**
     * Primitive fact: this answer was just graded. We don't track whether the attempt
     * is now fully graded — that's {@code AttemptGradingStateTracker}'s job.
     */
    private void publishAnswerGraded(TestAttemptEntity attempt, AssignmentEntity assignment) {
        eventPublisher.publishEvent(new AnswerGradedEvent(
                attempt.getId(),
                attempt.getTestEntity().getId(),
                attempt.getStudent().getId(),
                assignment.getId()
        ));
    }

    private void recalculateAttemptScore(TestAttemptEntity attempt) {
        Float answerScores = answerRepository.sumScoresByTestAttemptId(attempt.getId());
        Float codeScores = codeSubmissionRepository.sumScoresByTestAttemptId(attempt.getId());

        float total = 0f;
        if (answerScores != null) total += answerScores;
        if (codeScores != null) total += codeScores;

        attempt.setScore(total);
        testAttemptRepository.save(attempt);
    }

    private AnswerReviewDto buildAnswerReviewDto(
            AssignmentEntity assignment,
            TestAttemptEntity attempt,
            UserEntity student,
            AssignmentAnswerEntity answer) {

        AnswerReviewDto.AnswerReviewDtoBuilder builder = AnswerReviewDto.builder()
                .attemptId(attempt.getId())
                .assignmentId(assignment.getId())
                .assignmentTitle(assignment.getTitle())
                .assignmentType(assignment.getType().name())
                .assignmentDescription(assignment.getDescription())
                .maxPoints(assignment.getPoints().floatValue())
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName());

        if (answer != null) {
            builder.answerId(answer.getId())
                    .answeredAt(answer.getAnsweredAt())
                    .score(answer.getScore())
                    .isGraded(answer.getIsGraded())
                    .teacherFeedback(answer.getTeacherFeedback());

            switch (assignment.getType()) {
                case SINGLE_CHOICE -> {
                    SingleChoiceAnswerEntity singleAnswer = (SingleChoiceAnswerEntity) answer;
                    SingleChoiceAssignmentEntityEntity singleAssignment = (SingleChoiceAssignmentEntityEntity) assignment;

                    builder.selectedOptionId(singleAnswer.getSelectedOptionId());

                    List<ChoiceOptionEntity> options = choiceOptionRepository
                            .findByAssignmentIdOrderByOrderNumber(assignment.getId());
                    builder.options(mapOptions(options));
                    builder.correctOptionIds(options.stream()
                            .filter(ChoiceOptionEntity::isCorrectAnswer)
                            .map(ChoiceOptionEntity::getId)
                            .collect(Collectors.toList()));
                }
                case MULTIPLE_CHOICE -> {
                    MultipleChoiceAnswerEntity multiAnswer = (MultipleChoiceAnswerEntity) answer;

                    builder.selectedOptionIds(multiAnswer.getSelectedOptionIds());

                    List<ChoiceOptionEntity> options = choiceOptionRepository
                            .findByAssignmentIdOrderByOrderNumber(assignment.getId());
                    builder.options(mapOptions(options));
                    builder.correctOptionIds(options.stream()
                            .filter(ChoiceOptionEntity::isCorrectAnswer)
                            .map(ChoiceOptionEntity::getId)
                            .collect(Collectors.toList()));
                }
                case OPEN_QUESTION -> {
                    OpenQuestionAnswerEntity openAnswer = (OpenQuestionAnswerEntity) answer;
                    OpenQuestionAssignmentEntityEntity openAssignment = (OpenQuestionAssignmentEntityEntity) assignment;

                    builder.answerText(openAnswer.getAnswerText());
                    builder.sampleAnswer(openAssignment.getSampleAnswer());
                    builder.gradingRubric(openAssignment.getGradingRubric());
                }
            }
        } else {
            builder.isGraded(false);

            // Still provide assignment info even if not answered
            if (assignment.getType() == AssignmentType.OPEN_QUESTION) {
                OpenQuestionAssignmentEntityEntity openAssignment = (OpenQuestionAssignmentEntityEntity) assignment;
                builder.sampleAnswer(openAssignment.getSampleAnswer());
                builder.gradingRubric(openAssignment.getGradingRubric());
            } else if (assignment.getType() == AssignmentType.SINGLE_CHOICE ||
                    assignment.getType() == AssignmentType.MULTIPLE_CHOICE) {
                List<ChoiceOptionEntity> options = choiceOptionRepository
                        .findByAssignmentIdOrderByOrderNumber(assignment.getId());
                builder.options(mapOptions(options));
                builder.correctOptionIds(options.stream()
                        .filter(ChoiceOptionEntity::isCorrectAnswer)
                        .map(ChoiceOptionEntity::getId)
                        .collect(Collectors.toList()));
            }
        }

        return builder.build();
    }

    private AnswerReviewDto buildCodingReviewDto(
            AssignmentEntity assignment,
            TestAttemptEntity attempt,
            UserEntity student,
            CodeSubmissionEntity submission) {

        AnswerReviewDto.AnswerReviewDtoBuilder builder = AnswerReviewDto.builder()
                .attemptId(attempt.getId())
                .assignmentId(assignment.getId())
                .assignmentTitle(assignment.getTitle())
                .assignmentType(AssignmentType.CODING.name())
                .assignmentDescription(assignment.getDescription())
                .maxPoints(assignment.getPoints().floatValue())
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName());

        if (submission != null) {
            List<TestCaseResultEntity> results = submission.getTestCaseResults() != null
                    ? submission.getTestCaseResults() : List.of();
            List<TestCaseResultDto> resultDtos = results.stream()
                    .map(ManualGradingService::mapTestCaseResultForTeacher)
                    .collect(Collectors.toList());
            int passed = (int) results.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getPassed()))
                    .count();

            builder.answerId(submission.getId())
                    .answeredAt(submission.getSubmittedAt())
                    .sourceCode(submission.getSourceCode())
                    .programmingLanguage(submission.getProgrammingLanguage())
                    .compilationStatus(submission.getCompilationStatus() != null
                            ? submission.getCompilationStatus().name() : null)
                    .compilationError(submission.getCompilationError())
                    .executionStatus(submission.getExecutionStatus() != null
                            ? submission.getExecutionStatus().name() : null)
                    .testCaseResults(resultDtos)
                    .testCasesPassed(passed)
                    .testCasesTotal(results.size())
                    .score(submission.getTotalScore())
                    .isGraded(submission.getTotalScore() != null)
                    .teacherFeedback(submission.getTeacherFeedback());
        } else {
            builder.isGraded(false);
        }

        return builder.build();
    }

    private static TestCaseResultDto mapTestCaseResultForTeacher(TestCaseResultEntity result) {
        boolean isPublic = result.getTestCase() != null
                && Boolean.TRUE.equals(result.getTestCase().getIsPublic());
        return TestCaseResultDto.builder()
                .testCaseId(result.getTestCase() != null ? result.getTestCase().getId() : null)
                .isPublic(isPublic)
                .description(result.getTestCase() != null ? result.getTestCase().getDescription() : null)
                .inputData(result.getTestCase() != null ? result.getTestCase().getInputData() : null)
                .expectedOutput(result.getTestCase() != null ? result.getTestCase().getExpectedOutput() : null)
                .actualOutput(result.getActualOutput())
                .passed(result.getPassed())
                .executionTimeMs(result.getExecutionTimeMs())
                .memoryUsedMb(result.getMemoryUsedMb())
                .errorMessage(result.getErrorMessage())
                .build();
    }

    private List<ChoiceOptionDto> mapOptions(List<ChoiceOptionEntity> options) {
        if (options == null) {
            return Collections.emptyList();
        }
        return options.stream()
                .map(opt -> ChoiceOptionDto.builder()
                        .id(opt.getId())
                        .optionText(opt.getOptionText())
                        .correct(opt.isCorrectAnswer())
                        .orderNumber(opt.getOrderNumber())
                        .explanation(opt.getExplanation())
                        .build())
                .collect(Collectors.toList());
    }
}
