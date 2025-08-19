package com.edutest.service;


import com.edutest.domain.assignment.Assignment;
import com.edutest.domain.assignment.ValidationResult;
import com.edutest.domain.assignment.coding.CodingAssignment;

import com.edutest.domain.assignment.coding.TestCaseExecutionResult;
import com.edutest.domain.assignment.common.AssignmentAnswer;
import com.edutest.domain.assignment.common.ChoiceOption;
import com.edutest.domain.assignment.multiplechoice.MultipleChoiceAnswer;

import com.edutest.domain.assignment.multiplechoice.MultipleChoiceAssignment;
import com.edutest.domain.assignment.singlechoice.SingleChoiceAnswer;

import com.edutest.domain.assignment.openquestion.OpenQuestionAnswer;


import com.edutest.domain.assignment.singlechoice.SingleChoiceAssignment;
import com.edutest.dto.BatchGradingResultDto;
import com.edutest.dto.GradingResultDto;
import com.edutest.service.codiingassigment.CodingAssignmentScoringService;
import com.edutest.service.multiplechoiceanswear.MultipleChoiceGradingService;
import com.edutest.service.singlechoiceassigment.SingleChoiceGradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AssignmentGradingService {

    private final CodingAssignmentScoringService codingScoringService;
    private final MultipleChoiceGradingService multipleChoiceGradingService;
    private final SingleChoiceGradingService singleChoiceGradingService;

    @Autowired
    public AssignmentGradingService(CodingAssignmentScoringService codingScoringService,
                                    MultipleChoiceGradingService multipleChoiceGradingService,
                                    SingleChoiceGradingService singleChoiceGradingService) {
        this.codingScoringService = codingScoringService;
        this.multipleChoiceGradingService = multipleChoiceGradingService;
        this.singleChoiceGradingService = singleChoiceGradingService;
    }

    public GradingResultDto gradeAssignment(Assignment assignment, String answer) {
        ValidationResult validation = assignment.validateAnswer(answer);

        if (validation.hasError()) {
            return GradingResultDto.invalid(validation.getErrorMessage());
        }

        Float score = assignment.calculateScore(answer);
        Float maxScore = assignment.getPoints().floatValue();
        Float percentage = maxScore > 0 ? (score / maxScore) * 100 : 0;

        return GradingResultDto.builder()
                .valid(true)
                .score(score)
                .maxScore(maxScore)
                .percentage(percentage)
                .perfectScore(score.equals(maxScore))
                .build();
    }

    public GradingResultDto gradeCodingAssignment(CodingAssignment assignment,
                                                  List<TestCaseExecutionResult> testResults) {
        if (testResults == null || testResults.isEmpty()) {
            return GradingResultDto.invalid("No test execution results provided");
        }

        Float score = codingScoringService.calculateScoreFromTestResults(assignment, testResults);
        Float maxScore = assignment.getPoints().floatValue();
        Float percentage = maxScore > 0 ? (score / maxScore) * 100 : 0;

        return GradingResultDto.builder()
                .valid(true)
                .score(score)
                .maxScore(maxScore)
                .percentage(percentage)
                .perfectScore(score.equals(maxScore))
                .build();
    }

    public MultipleChoiceAnswer autoGradeMultipleChoice(Long answerId, Long assignmentId) {
        MultipleChoiceAnswer answer = answerRepository.findById(answerId);
        MultipleChoiceAssignment assignment = assignmentRepository.findById(assignmentId);
        List<ChoiceOption> options = optionRepository.findByAssignmentId(assignmentId);

        if (answer.isGraded()) {
            throw new IllegalStateException("Answer is already graded");
        }

        return multipleChoiceGradingService.autoGrade(answer, assignment, options);
    }


    public SingleChoiceAnswer autoGradeSingleChoice(Long answerId, Long assignmentId) {
        SingleChoiceAnswer answer = answerRepository.findById(answerId);
        SingleChoiceAssignment assignment = assignmentRepository.findById(assignmentId);
        List<ChoiceOption> options = optionRepository.findByAssignmentId(assignmentId);

        if (answer.isGraded()) {
            throw new IllegalStateException("Answer is already graded");
        }

        return singleChoiceGradingService.autoGrade(answer, assignment, options);
    }

    public OpenQuestionAnswer gradeOpenQuestion(Long answerId, Float score, String feedback) {
        OpenQuestionAnswer answer = answerRepository.findById(answerId);

        return answer.toBuilder()
                .score(score)
                .teacherFeedback(feedback)
                .graded(true)
                .build();
    }

    public BatchGradingResultDto batchAutoGrade(List<Long> answerIds) {
        int totalAnswers = answerIds.size();
        int gradedCount = 0;
        int errorCount = 0;
        int skippedCount = 0;

        for (Long answerId : answerIds) {
            try {
                AssignmentAnswer answer = answerRepository.findById(answerId);

                if (canBeAutoGraded(answer)) {
                    AssignmentAnswer gradedAnswer = autoGradeByType(answer);
                    answerRepository.save(gradedAnswer);
                    gradedCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                errorCount++;
            }
        }

        return BatchGradingResultDto.builder()
                .totalAnswers(totalAnswers)
                .gradedCount(gradedCount)
                .errorCount(errorCount)
                .skippedCount(skippedCount)
                .build();
    }

    private boolean canBeAutoGraded(AssignmentAnswer answer) {
        return answer.hasAnswer() &&
                !answer.isGraded() &&
                isAutoGradeableType(answer);
    }

    private boolean isAutoGradeableType(AssignmentAnswer answer) {
        return answer instanceof MultipleChoiceAnswer ||
                answer instanceof SingleChoiceAnswer ||
                answer instanceof CodingAnswer;
    }

    private AssignmentAnswer autoGradeByType(AssignmentAnswer answer) {
        return switch (answer) {
            case MultipleChoiceAnswer mcAnswer ->
                    autoGradeMultipleChoice(mcAnswer.getId(), mcAnswer.getAssignmentId());
            case SingleChoiceAnswer scAnswer ->
                    autoGradeSingleChoice(scAnswer.getId(), scAnswer.getAssignmentId());
            default -> throw new IllegalArgumentException("Cannot auto-grade this answer type");
        };
    }
}