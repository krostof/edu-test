package com.edutest.service.answer;

import com.edutest.dto.AnswerDto;
import com.edutest.dto.AssignmentResultDto;
import com.edutest.dto.TestResultResponseDto;
import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.assigment.AssignmentType;
import com.edutest.persistance.entity.assigment.common.AssignmentAnswerEntity;
import com.edutest.persistance.entity.assigment.multiplechoice.MultipleChoiceAnswerEntity;
import com.edutest.persistance.entity.assigment.openquestion.OpenQuestionAnswerEntity;
import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAnswerEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestResultsService {

    private final TestAttemptJpaRepository testAttemptRepository;
    private final AssignmentJpaRepository assignmentRepository;
    private final AssignmentAnswerJpaRepository answerRepository;
    private final CodeSubmissionJpaRepository codeSubmissionRepository;

    @Transactional(readOnly = true)
    public TestResultResponseDto getTestResults(Long testId, Long attemptId, Long studentId) {
        TestAttemptEntity attempt = testAttemptRepository.findByIdWithTestAndStudent(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Test attempt not found"));

        if (!attempt.getTestEntity().getId().equals(testId)) {
            throw new IllegalArgumentException("Test attempt does not belong to this test");
        }

        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("You do not have access to this test attempt");
        }

        if (!attempt.isFinished()) {
            throw new AccessDeniedException("Test results are not available until the test is completed");
        }

        List<AssignmentEntity> assignments = assignmentRepository.findByTestEntityIdOrderByOrderNumber(testId);

        Map<Long, AssignmentAnswerEntity> answersByAssignment = answerRepository
                .findByTestAttemptId(attemptId).stream()
                .collect(Collectors.toMap(
                        a -> a.getAssignmentEntity().getId(),
                        Function.identity()));

        Map<Long, CodeSubmissionEntity> codeSubmissionsByAssignment = codeSubmissionRepository
                .findByTestAttemptId(attemptId).stream()
                .collect(Collectors.toMap(
                        c -> c.getAssignment().getId(),
                        Function.identity()));

        List<AssignmentResultDto> assignmentResults = new ArrayList<>();

        for (AssignmentEntity assignment : assignments) {
            AssignmentResultDto result = buildAssignmentResult(
                    assignment,
                    answersByAssignment.get(assignment.getId()),
                    codeSubmissionsByAssignment.get(assignment.getId()));
            assignmentResults.add(result);
        }

        Float maxPossibleScore = assignmentRepository.sumPointsByTestId(testId);
        boolean fullyGraded = assignmentResults.stream()
                .allMatch(r -> Boolean.TRUE.equals(r.getIsGraded()));

        return TestResultResponseDto.create(
                attemptId,
                testId,
                attempt.getTestEntity().getTitle(),
                studentId,
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                attempt.getScore(),
                maxPossibleScore != null ? maxPossibleScore.floatValue() : 0f,
                fullyGraded,
                assignmentResults
        );
    }

    private AssignmentResultDto buildAssignmentResult(
            AssignmentEntity assignment,
            AssignmentAnswerEntity answer,
            CodeSubmissionEntity codeSubmission) {

        AssignmentType type = assignment.getType();

        if (type == AssignmentType.CODING) {
            return buildCodingAssignmentResult(assignment, codeSubmission);
        } else {
            return buildNonCodingAssignmentResult(assignment, answer);
        }
    }

    private AssignmentResultDto buildNonCodingAssignmentResult(
            AssignmentEntity assignment,
            AssignmentAnswerEntity answer) {

        if (answer == null) {
            return AssignmentResultDto.notAnswered(
                    assignment.getId(),
                    assignment.getTitle(),
                    assignment.getType().name(),
                    assignment.getOrderNumber(),
                    assignment.getPoints().floatValue()
            );
        }

        AnswerDto answerDto = mapAnswerToDto(answer);

        return AssignmentResultDto.builder()
                .assignmentId(assignment.getId())
                .assignmentTitle(assignment.getTitle())
                .assignmentType(assignment.getType().name())
                .orderNumber(assignment.getOrderNumber())
                .maxPoints(assignment.getPoints().floatValue())
                .earnedScore(answer.getScore() != null ? answer.getScore() : 0f)
                .isCorrect(answer.isCorrect())
                .isGraded(answer.getIsGraded())
                .teacherFeedback(answer.getTeacherFeedback())
                .answer(answerDto)
                .build();
    }

    private AssignmentResultDto buildCodingAssignmentResult(
            AssignmentEntity assignment,
            CodeSubmissionEntity submission) {

        if (submission == null) {
            return AssignmentResultDto.notAnswered(
                    assignment.getId(),
                    assignment.getTitle(),
                    AssignmentType.CODING.name(),
                    assignment.getOrderNumber(),
                    assignment.getPoints().floatValue()
            );
        }

        AnswerDto answerDto = mapCodeSubmissionToDto(submission);

        float earnedScore = submission.getTotalScore() != null ? submission.getTotalScore() : 0f;
        boolean isGraded = submission.getTotalScore() != null;

        return AssignmentResultDto.builder()
                .assignmentId(assignment.getId())
                .assignmentTitle(assignment.getTitle())
                .assignmentType(AssignmentType.CODING.name())
                .orderNumber(assignment.getOrderNumber())
                .maxPoints(assignment.getPoints().floatValue())
                .earnedScore(earnedScore)
                .isCorrect(earnedScore >= assignment.getPoints())
                .isGraded(isGraded)
                .answer(answerDto)
                .build();
    }

    private AnswerDto mapAnswerToDto(AssignmentAnswerEntity answer) {
        AnswerDto.AnswerDtoBuilder builder = AnswerDto.builder()
                .id(answer.getId())
                .assignmentId(answer.getAssignmentEntity().getId())
                .assignmentType(answer.getAssignmentEntity().getType().name())
                .answeredAt(answer.getAnsweredAt())
                .score(answer.getScore())
                .isGraded(answer.getIsGraded())
                .teacherFeedback(answer.getTeacherFeedback());

        if (answer instanceof SingleChoiceAnswerEntity singleChoice) {
            builder.selectedOptionId(singleChoice.getSelectedOptionId());
        } else if (answer instanceof MultipleChoiceAnswerEntity multipleChoice) {
            builder.selectedOptionIds(multipleChoice.getSelectedOptionIds());
        } else if (answer instanceof OpenQuestionAnswerEntity openQuestion) {
            builder.answerText(openQuestion.getAnswerText());
            builder.wordCount(openQuestion.getWordCount());
            builder.characterCount(openQuestion.getCharacterCount());
        }

        return builder.build();
    }

    private AnswerDto mapCodeSubmissionToDto(CodeSubmissionEntity submission) {
        return AnswerDto.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .assignmentType(AssignmentType.CODING.name())
                .answeredAt(submission.getSubmittedAt())
                .sourceCode(submission.getSourceCode())
                .programmingLanguage(submission.getProgrammingLanguage())
                .compilationStatus(submission.getCompilationStatus() != null
                        ? submission.getCompilationStatus().name() : null)
                .compilationError(submission.getCompilationError())
                .executionStatus(submission.getExecutionStatus() != null
                        ? submission.getExecutionStatus().name() : null)
                .score(submission.getTotalScore())
                .isGraded(submission.getTotalScore() != null)
                .build();
    }
}
