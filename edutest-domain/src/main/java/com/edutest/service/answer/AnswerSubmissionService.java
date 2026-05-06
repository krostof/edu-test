package com.edutest.service.answer;

import com.edutest.dto.AnswerDto;
import com.edutest.dto.SubmitAnswerRequestDto;
import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.assigment.AssignmentType;
import com.edutest.persistance.entity.assigment.coding.CodingAssignmentEntity;
import com.edutest.persistance.entity.assigment.common.AssignmentAnswerEntity;
import com.edutest.persistance.entity.assigment.common.ChoiceOptionEntity;
import com.edutest.persistance.entity.assigment.multiplechoice.MultipleChoiceAnswerEntity;
import com.edutest.persistance.entity.assigment.openquestion.OpenQuestionAnswerEntity;
import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAnswerEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.*;
import com.edutest.service.attempt.AttemptRandomizationService;
import com.edutest.service.codeexecution.CodeExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerSubmissionService {

    private final TestAttemptJpaRepository testAttemptRepository;
    private final AssignmentJpaRepository assignmentRepository;
    private final AssignmentAnswerJpaRepository answerRepository;
    private final CodeSubmissionJpaRepository codeSubmissionRepository;
    private final ChoiceOptionJpaRepository choiceOptionRepository;
    private final UserRepository userRepository;
    private final AttemptRandomizationService randomizationService;
    private final CodeExecutionService codeExecutionService;

    @Transactional
    public AnswerDto submitAnswer(Long testId, Long attemptId, Long assignmentId, Long studentId, SubmitAnswerRequestDto request) {
        TestAttemptEntity attempt = validateAndGetAttempt(testId, attemptId, studentId);
        AssignmentEntity assignment = validateAndGetAssignment(testId, assignmentId);

        // Validate navigation restrictions
        validateNavigationRestrictions(attempt, assignmentId);

        AssignmentType type = assignment.getType();

        if (type == AssignmentType.CODING) {
            CodeSubmissionEntity submission = submitCodeAnswer(attempt, (CodingAssignmentEntity) assignment, studentId, request);
            return mapCodeSubmissionToDto(submission);
        } else {
            AssignmentAnswerEntity answer = submitNonCodeAnswer(attempt, assignment, studentId, request);
            return mapAnswerToDto(answer);
        }
    }

    private void validateNavigationRestrictions(TestAttemptEntity attempt, Long assignmentId) {
        // If navigation is allowed, no restrictions
        if (Boolean.TRUE.equals(attempt.getTestEntity().getAllowNavigation())) {
            return;
        }

        // Get the assignment's position in the randomized order
        List<Long> assignmentOrder = randomizationService.getAssignmentOrder(attempt);
        if (assignmentOrder.isEmpty()) {
            // Fallback to original order if no randomization data
            return;
        }

        int questionIndex = assignmentOrder.indexOf(assignmentId);
        if (questionIndex == -1) {
            log.warn("Assignment {} not found in order for attempt {}", assignmentId, attempt.getId());
            return;
        }

        Integer currentIndex = attempt.getCurrentQuestionIndex();
        if (currentIndex == null) {
            currentIndex = 0;
        }

        // Cannot go back to previous questions
        if (questionIndex < currentIndex) {
            throw new IllegalStateException("Navigation to previous questions is not allowed. Current question: " +
                    (currentIndex + 1) + ", attempted question: " + (questionIndex + 1));
        }

        // Update current question index if moving forward
        if (questionIndex > currentIndex) {
            attempt.setCurrentQuestionIndex(questionIndex);
            testAttemptRepository.save(attempt);
            log.debug("Updated current question index to {} for attempt {}", questionIndex, attempt.getId());
        }
    }

    @Transactional(readOnly = true)
    public Optional<AnswerDto> getAnswer(Long testId, Long attemptId, Long assignmentId, Long studentId) {
        validateAndGetAttempt(testId, attemptId, studentId);
        AssignmentEntity assignment = validateAndGetAssignment(testId, assignmentId);

        if (assignment.getType() == AssignmentType.CODING) {
            return codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(attemptId, assignmentId)
                    .map(this::mapCodeSubmissionToDto);
        } else {
            return answerRepository.findByTestAttemptIdAndAssignmentId(attemptId, assignmentId)
                    .map(this::mapAnswerToDto);
        }
    }

    @Transactional(readOnly = true)
    public List<AnswerDto> getAllAnswers(Long testId, Long attemptId, Long studentId) {
        validateAndGetAttempt(testId, attemptId, studentId);

        List<AnswerDto> answers = new ArrayList<>();

        List<AssignmentAnswerEntity> nonCodeAnswers = answerRepository.findByTestAttemptId(attemptId);
        answers.addAll(nonCodeAnswers.stream()
                .map(this::mapAnswerToDto)
                .collect(Collectors.toList()));

        List<CodeSubmissionEntity> codeAnswers = codeSubmissionRepository.findByTestAttemptId(attemptId);
        answers.addAll(codeAnswers.stream()
                .map(this::mapCodeSubmissionToDto)
                .collect(Collectors.toList()));

        return answers;
    }

    private TestAttemptEntity validateAndGetAttempt(Long testId, Long attemptId, Long studentId) {
        TestAttemptEntity attempt = testAttemptRepository.findByIdAndTestId(attemptId, testId)
                .orElseThrow(() -> new IllegalArgumentException("Test attempt not found"));

        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("You do not have access to this test attempt");
        }

        return attempt;
    }

    private void validateAttemptInProgress(TestAttemptEntity attempt) {
        if (attempt.isFinished()) {
            throw new IllegalStateException("Test attempt is already completed");
        }

        if (attempt.isTimeExpired()) {
            throw new IllegalStateException("Test time has expired");
        }
    }

    private AssignmentEntity validateAndGetAssignment(Long testId, Long assignmentId) {
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        if (!assignment.getTestEntity().getId().equals(testId)) {
            throw new IllegalArgumentException("Assignment does not belong to this test");
        }

        return assignment;
    }

    private AssignmentAnswerEntity submitNonCodeAnswer(
            TestAttemptEntity attempt,
            AssignmentEntity assignment,
            Long studentId,
            SubmitAnswerRequestDto request) {

        validateAttemptInProgress(attempt);

        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        Optional<AssignmentAnswerEntity> existingAnswer =
                answerRepository.findByTestAttemptIdAndAssignmentId(attempt.getId(), assignment.getId());

        AssignmentAnswerEntity answer;

        switch (assignment.getType()) {
            case SINGLE_CHOICE -> {
                answer = existingAnswer.map(a -> (SingleChoiceAnswerEntity) a)
                        .orElseGet(SingleChoiceAnswerEntity::new);
                updateSingleChoiceAnswer((SingleChoiceAnswerEntity) answer, request.getSelectedOptionId());
            }
            case MULTIPLE_CHOICE -> {
                answer = existingAnswer.map(a -> (MultipleChoiceAnswerEntity) a)
                        .orElseGet(MultipleChoiceAnswerEntity::new);
                updateMultipleChoiceAnswer((MultipleChoiceAnswerEntity) answer, request.getSelectedOptionIds());
            }
            case OPEN_QUESTION -> {
                answer = existingAnswer.map(a -> (OpenQuestionAnswerEntity) a)
                        .orElseGet(OpenQuestionAnswerEntity::new);
                updateOpenQuestionAnswer((OpenQuestionAnswerEntity) answer, request.getAnswerText());
            }
            default -> throw new IllegalArgumentException("Unsupported assignment type: " + assignment.getType());
        }

        if (existingAnswer.isEmpty()) {
            answer.setAssignmentEntity(assignment);
            answer.setTestAttemptEntity(attempt);
            answer.setStudent(student);
        }

        answer.setAnsweredAt(LocalDateTime.now());

        return answerRepository.save(answer);
    }

    private void updateSingleChoiceAnswer(SingleChoiceAnswerEntity answer, Long selectedOptionId) {
        if (selectedOptionId == null) {
            answer.setSelectedOption(null);
        } else {
            ChoiceOptionEntity option = choiceOptionRepository.findById(selectedOptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Option not found: " + selectedOptionId));
            answer.setSelectedOption(option);
        }
    }

    private void updateMultipleChoiceAnswer(MultipleChoiceAnswerEntity answer, List<Long> selectedOptionIds) {
        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            answer.setSelectedOptions(new ArrayList<>());
        } else {
            List<ChoiceOptionEntity> options = choiceOptionRepository.findAllByIds(selectedOptionIds);
            if (options.size() != selectedOptionIds.size()) {
                throw new IllegalArgumentException("One or more options not found");
            }
            answer.setSelectedOptions(options);
        }
    }

    private void updateOpenQuestionAnswer(OpenQuestionAnswerEntity answer, String answerText) {
        answer.setAnswerText(answerText);
        if (answerText != null) {
            answer.setCharacterCount(answerText.length());
            answer.setWordCount(countWords(answerText));
        } else {
            answer.setCharacterCount(0);
            answer.setWordCount(0);
        }
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private CodeSubmissionEntity submitCodeAnswer(
            TestAttemptEntity attempt,
            CodingAssignmentEntity assignment,
            Long studentId,
            SubmitAnswerRequestDto request) {

        validateAttemptInProgress(attempt);

        if (request.getSourceCode() == null || request.getSourceCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Source code is required for coding assignments");
        }

        if (request.getProgrammingLanguage() == null || request.getProgrammingLanguage().trim().isEmpty()) {
            throw new IllegalArgumentException("Programming language is required for coding assignments");
        }

        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        CodeSubmissionEntity submission = codeSubmissionRepository
                .findByTestAttemptIdAndAssignmentId(attempt.getId(), assignment.getId())
                .orElse(CodeSubmissionEntity.builder()
                        .assignment(assignment)
                        .testAttempt(attempt)
                        .student(student)
                        .build());

        submission.setSourceCode(request.getSourceCode());
        submission.setProgrammingLanguage(request.getProgrammingLanguage());
        submission.setSubmittedAt(LocalDateTime.now());

        CodeSubmissionEntity saved = codeSubmissionRepository.save(submission);

        try {
            codeExecutionService.executeAndPersist(saved);
        } catch (Exception e) {
            log.error("Code execution failed for submission {} (attempt {}, assignment {}): {}",
                    saved.getId(), attempt.getId(), assignment.getId(), e.getMessage(), e);
        }

        return codeSubmissionRepository.save(saved);
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
