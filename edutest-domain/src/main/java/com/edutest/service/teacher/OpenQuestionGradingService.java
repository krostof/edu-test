package com.edutest.service.teacher;

import com.edutest.dto.AnswerReviewDto;
import com.edutest.dto.ChoiceOptionDto;
import com.edutest.dto.GradeAnswerRequestDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenQuestionGradingService {

    private final TestAttemptJpaRepository testAttemptRepository;
    private final AssignmentJpaRepository assignmentRepository;
    private final AssignmentAnswerJpaRepository answerRepository;
    private final CodeSubmissionJpaRepository codeSubmissionRepository;
    private final ChoiceOptionJpaRepository choiceOptionRepository;

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
            codeSubmissionRepository.save(submission);

            recalculateAttemptScore(attempt);

            return buildCodingReviewDto(assignment, attempt, attempt.getStudent(), submission);
        } else {
            AssignmentAnswerEntity answer = answerRepository
                    .findByTestAttemptIdAndAssignmentId(attemptId, assignmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Answer not found"));

            answer.grade(request.getScore(), request.getFeedback());
            answerRepository.save(answer);

            recalculateAttemptScore(attempt);

            return buildAnswerReviewDto(assignment, attempt, attempt.getStudent(), answer);
        }
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
            builder.answerId(submission.getId())
                    .answeredAt(submission.getSubmittedAt())
                    .sourceCode(submission.getSourceCode())
                    .programmingLanguage(submission.getProgrammingLanguage())
                    .score(submission.getTotalScore())
                    .isGraded(submission.getTotalScore() != null);
        } else {
            builder.isGraded(false);
        }

        return builder.build();
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
