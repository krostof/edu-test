package com.edutest.service.attempt;

import com.edutest.dto.PreviousAnswerDto;
import com.edutest.dto.QuestionOptionDto;
import com.edutest.dto.QuestionViewDto;
import com.edutest.dto.TestAttemptStateDto;
import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.assigment.AssignmentType;
import com.edutest.persistance.entity.assigment.coding.CodingAssignmentEntity;
import com.edutest.persistance.entity.assigment.common.AssignmentAnswerEntity;
import com.edutest.persistance.entity.assigment.common.ChoiceOptionEntity;
import com.edutest.persistance.entity.assigment.multiplechoice.MultipleChoiceAnswerEntity;
import com.edutest.persistance.entity.assigment.multiplechoice.MultipleChoiceAssignmentEntity;
import com.edutest.persistance.entity.assigment.openquestion.OpenQuestionAnswerEntity;
import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAnswerEntity;
import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAssignmentEntityEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.repository.AssignmentAnswerJpaRepository;
import com.edutest.persistance.repository.AssignmentJpaRepository;
import com.edutest.persistance.repository.CodeSubmissionJpaRepository;
import com.edutest.persistance.repository.TestAttemptJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestAttemptStateService {

    private final TestAttemptJpaRepository testAttemptRepository;
    private final AssignmentJpaRepository assignmentRepository;
    private final AssignmentAnswerJpaRepository answerRepository;
    private final CodeSubmissionJpaRepository codeSubmissionRepository;
    private final AttemptRandomizationService randomizationService;

    @Transactional(readOnly = true)
    public TestAttemptStateDto getAttemptState(Long testId, Long attemptId, Long studentId) {
        TestAttemptEntity attempt = testAttemptRepository.findByIdWithTestAndStudent(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Test attempt not found"));

        validateAccess(attempt, testId, studentId);

        TestEntity test = attempt.getTestEntity();
        List<Long> assignmentOrder = randomizationService.getAssignmentOrder(attempt);

        // Get answered assignment IDs
        List<Long> answeredIds = getAnsweredAssignmentIds(attemptId);

        // Calculate remaining time
        Long remainingTimeSeconds = calculateRemainingTimeSeconds(attempt, test);

        return TestAttemptStateDto.builder()
                .attemptId(attemptId)
                .testId(testId)
                .testTitle(test.getTitle())
                .currentQuestionIndex(attempt.getCurrentQuestionIndex() != null ? attempt.getCurrentQuestionIndex() : 0)
                .totalQuestions(assignmentOrder.size())
                .remainingTimeSeconds(remainingTimeSeconds)
                .startedAt(attempt.getStartedAt())
                .isCompleted(attempt.isFinished())
                .allowNavigation(Boolean.TRUE.equals(test.getAllowNavigation()))
                .assignmentOrder(assignmentOrder)
                .answeredAssignmentIds(answeredIds)
                .build();
    }

    @Transactional(readOnly = true)
    public QuestionViewDto getQuestionByIndex(Long testId, Long attemptId, int questionIndex, Long studentId) {
        TestAttemptEntity attempt = testAttemptRepository.findByIdWithTestAndStudent(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Test attempt not found"));

        validateAccess(attempt, testId, studentId);
        validateAttemptInProgress(attempt);
        validateQuestionAccess(attempt, questionIndex);

        List<Long> assignmentOrder = randomizationService.getAssignmentOrder(attempt);

        if (questionIndex < 0 || questionIndex >= assignmentOrder.size()) {
            throw new IllegalArgumentException("Invalid question index: " + questionIndex);
        }

        Long assignmentId = assignmentOrder.get(questionIndex);
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        // Get options with randomization if applicable
        List<QuestionOptionDto> options = getOptionsForQuestion(attempt, assignment);

        // Get previous answer if any
        PreviousAnswerDto previousAnswer = getPreviousAnswer(attemptId, assignmentId, assignment.getType());

        QuestionViewDto.QuestionViewDtoBuilder builder = QuestionViewDto.builder()
                .assignmentId(assignmentId)
                .questionIndex(questionIndex)
                .totalQuestions(assignmentOrder.size())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .assignmentType(assignment.getType().name())
                .points(assignment.getPoints())
                .options(options)
                .previousAnswer(previousAnswer);

        // Add coding-specific fields
        if (assignment instanceof CodingAssignmentEntity codingAssignment) {
            builder.programmingLanguage(codingAssignment.getAllowedLanguagesStr());
            builder.starterCode(codingAssignment.getStarterCode());
        }

        return builder.build();
    }

    @Transactional
    public void updateNavigation(Long testId, Long attemptId, int questionIndex, Long studentId) {
        TestAttemptEntity attempt = testAttemptRepository.findByIdWithTest(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Test attempt not found"));

        validateAccess(attempt, testId, studentId);
        validateAttemptInProgress(attempt);
        validateNavigationAllowed(attempt, questionIndex);

        List<Long> assignmentOrder = randomizationService.getAssignmentOrder(attempt);
        if (questionIndex < 0 || questionIndex >= assignmentOrder.size()) {
            throw new IllegalArgumentException("Invalid question index: " + questionIndex);
        }

        attempt.setCurrentQuestionIndex(questionIndex);
        testAttemptRepository.save(attempt);

        log.debug("Updated navigation to index {} for attempt {}", questionIndex, attemptId);
    }

    private void validateAccess(TestAttemptEntity attempt, Long testId, Long studentId) {
        if (!attempt.getTestEntity().getId().equals(testId)) {
            throw new IllegalArgumentException("Test attempt does not belong to this test");
        }

        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("You do not have access to this test attempt");
        }
    }

    private void validateAttemptInProgress(TestAttemptEntity attempt) {
        if (attempt.isFinished()) {
            throw new IllegalStateException("Test attempt is already completed");
        }

        if (attempt.isTimeExpired()) {
            throw new IllegalStateException("Test time has expired");
        }
    }

    private void validateQuestionAccess(TestAttemptEntity attempt, int questionIndex) {
        TestEntity test = attempt.getTestEntity();

        // If navigation is allowed, can access any question
        if (Boolean.TRUE.equals(test.getAllowNavigation())) {
            return;
        }

        // If navigation is not allowed, can only access current or future questions
        Integer currentIndex = attempt.getCurrentQuestionIndex();
        if (currentIndex == null) {
            currentIndex = 0;
        }

        if (questionIndex < currentIndex) {
            throw new AccessDeniedException("Navigation to previous questions is not allowed");
        }
    }

    private void validateNavigationAllowed(TestAttemptEntity attempt, int targetIndex) {
        TestEntity test = attempt.getTestEntity();

        // If navigation is allowed, can navigate anywhere
        if (Boolean.TRUE.equals(test.getAllowNavigation())) {
            return;
        }

        // If navigation is not allowed, can only move forward
        Integer currentIndex = attempt.getCurrentQuestionIndex();
        if (currentIndex == null) {
            currentIndex = 0;
        }

        if (targetIndex < currentIndex) {
            throw new AccessDeniedException("Navigation to previous questions is not allowed for this test");
        }
    }

    private List<Long> getAnsweredAssignmentIds(Long attemptId) {
        List<Long> answeredIds = new ArrayList<>();

        List<AssignmentAnswerEntity> answers = answerRepository.findByTestAttemptId(attemptId);
        answeredIds.addAll(answers.stream()
                .map(a -> a.getAssignmentEntity().getId())
                .collect(Collectors.toList()));

        List<CodeSubmissionEntity> codeSubmissions = codeSubmissionRepository.findByTestAttemptId(attemptId);
        answeredIds.addAll(codeSubmissions.stream()
                .map(c -> c.getAssignment().getId())
                .collect(Collectors.toList()));

        return answeredIds;
    }

    private Long calculateRemainingTimeSeconds(TestAttemptEntity attempt, TestEntity test) {
        if (test.getTimeLimit() == null) {
            return null;
        }

        long elapsedSeconds = ChronoUnit.SECONDS.between(attempt.getStartedAt(), LocalDateTime.now());
        long timeLimitSeconds = test.getTimeLimit() * 60L;
        long remaining = timeLimitSeconds - elapsedSeconds;

        return Math.max(0, remaining);
    }

    private List<QuestionOptionDto> getOptionsForQuestion(TestAttemptEntity attempt, AssignmentEntity assignment) {
        List<ChoiceOptionEntity> options = null;

        if (assignment instanceof SingleChoiceAssignmentEntityEntity singleChoice) {
            options = singleChoice.getOptions();
        } else if (assignment instanceof MultipleChoiceAssignmentEntity multipleChoice) {
            options = multipleChoice.getOptions();
        }

        if (options == null || options.isEmpty()) {
            return List.of();
        }

        // Get randomized order if applicable
        List<Long> optionsOrder = randomizationService.getOptionsOrderForAssignment(attempt, assignment.getId());

        List<QuestionOptionDto> result;

        if (!optionsOrder.isEmpty()) {
            // Use randomized order
            Map<Long, ChoiceOptionEntity> optionsMap = options.stream()
                    .collect(Collectors.toMap(ChoiceOptionEntity::getId, o -> o));

            result = new ArrayList<>();
            int orderNum = 1;
            for (Long optionId : optionsOrder) {
                ChoiceOptionEntity option = optionsMap.get(optionId);
                if (option != null) {
                    result.add(QuestionOptionDto.builder()
                            .id(option.getId())
                            .text(option.getOptionText())
                            .orderNumber(orderNum++)
                            .build());
                }
            }
        } else {
            // Use original order
            result = options.stream()
                    .sorted(Comparator.comparing(ChoiceOptionEntity::getOrderNumber))
                    .map(option -> QuestionOptionDto.builder()
                            .id(option.getId())
                            .text(option.getOptionText())
                            .orderNumber(option.getOrderNumber())
                            .build())
                    .collect(Collectors.toList());
        }

        return result;
    }

    private PreviousAnswerDto getPreviousAnswer(Long attemptId, Long assignmentId, AssignmentType type) {
        if (type == AssignmentType.CODING) {
            return codeSubmissionRepository.findByTestAttemptIdAndAssignmentId(attemptId, assignmentId)
                    .map(this::mapCodeSubmissionToAnswer)
                    .orElse(null);
        } else {
            return answerRepository.findByTestAttemptIdAndAssignmentId(attemptId, assignmentId)
                    .map(this::mapAnswerToDto)
                    .orElse(null);
        }
    }

    private PreviousAnswerDto mapAnswerToDto(AssignmentAnswerEntity answer) {
        PreviousAnswerDto.PreviousAnswerDtoBuilder builder = PreviousAnswerDto.builder()
                .answeredAt(answer.getAnsweredAt());

        if (answer instanceof SingleChoiceAnswerEntity singleChoice) {
            builder.selectedOptionId(singleChoice.getSelectedOptionId());
        } else if (answer instanceof MultipleChoiceAnswerEntity multipleChoice) {
            builder.selectedOptionIds(multipleChoice.getSelectedOptionIds());
        } else if (answer instanceof OpenQuestionAnswerEntity openQuestion) {
            builder.answerText(openQuestion.getAnswerText());
        }

        return builder.build();
    }

    private PreviousAnswerDto mapCodeSubmissionToAnswer(CodeSubmissionEntity submission) {
        return PreviousAnswerDto.builder()
                .sourceCode(submission.getSourceCode())
                .programmingLanguage(submission.getProgrammingLanguage())
                .answeredAt(submission.getSubmittedAt())
                .build();
    }
}
