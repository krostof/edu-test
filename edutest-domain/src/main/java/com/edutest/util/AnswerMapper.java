package com.edutest.util;

import com.edutest.api.model.*;
import com.edutest.dto.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AnswerMapper {

    public AnswerResponse toApiAnswerResponse(AnswerDto dto) {
        if (dto == null) {
            return null;
        }

        AnswerResponse response = new AnswerResponse();
        response.setId(dto.getId());
        response.setAssignmentId(dto.getAssignmentId());
        response.setAssignmentType(mapAssignmentType(dto.getAssignmentType()));
        response.setAnsweredAt(toOffsetDateTime(dto.getAnsweredAt()));

        response.setSelectedOptionId(dto.getSelectedOptionId());
        response.setSelectedOptionIds(dto.getSelectedOptionIds());

        response.setAnswerText(dto.getAnswerText());
        response.setWordCount(dto.getWordCount());
        response.setCharacterCount(dto.getCharacterCount());

        response.setSourceCode(dto.getSourceCode());
        response.setProgrammingLanguage(dto.getProgrammingLanguage());
        response.setCompilationStatus(mapCompilationStatus(dto.getCompilationStatus()));
        response.setCompilationError(dto.getCompilationError());
        response.setExecutionStatus(mapExecutionStatus(dto.getExecutionStatus()));

        response.setScore(dto.getScore());
        response.setIsGraded(dto.getIsGraded());
        response.setTeacherFeedback(dto.getTeacherFeedback());

        return response;
    }

    public TestSubmissionResult toApiTestSubmissionResult(TestSubmissionResultDto dto) {
        if (dto == null) {
            return null;
        }

        TestSubmissionResult result = new TestSubmissionResult();
        result.setAttemptId(dto.getAttemptId());
        result.setTestId(dto.getTestId());
        result.setSubmittedAt(toOffsetDateTime(dto.getSubmittedAt()));
        result.setTotalScore(dto.getTotalScore());
        result.setMaxPossibleScore(dto.getMaxPossibleScore());
        result.setScorePercentage(dto.getScorePercentage());
        result.setGradedCount(dto.getGradedCount());
        result.setPendingGradingCount(dto.getPendingGradingCount());

        return result;
    }

    public TestResultResponse toApiTestResultResponse(TestResultResponseDto dto) {
        if (dto == null) {
            return null;
        }

        TestResultResponse response = new TestResultResponse();
        response.setAttemptId(dto.getAttemptId());
        response.setTestId(dto.getTestId());
        response.setTestTitle(dto.getTestTitle());
        response.setStudentId(dto.getStudentId());
        response.setStartedAt(toOffsetDateTime(dto.getStartedAt()));
        response.setFinishedAt(toOffsetDateTime(dto.getFinishedAt()));
        response.setTotalScore(dto.getTotalScore());
        response.setMaxPossibleScore(dto.getMaxPossibleScore());
        response.setScorePercentage(dto.getScorePercentage());
        response.setIsFullyGraded(dto.isFullyGraded());

        if (dto.getAssignmentResults() != null) {
            List<AssignmentResultResponse> assignmentResults = dto.getAssignmentResults().stream()
                    .map(this::toApiAssignmentResultResponse)
                    .collect(Collectors.toList());
            response.setAssignmentResults(assignmentResults);
        }

        return response;
    }

    public AssignmentResultResponse toApiAssignmentResultResponse(AssignmentResultDto dto) {
        if (dto == null) {
            return null;
        }

        AssignmentResultResponse response = new AssignmentResultResponse();
        response.setAssignmentId(dto.getAssignmentId());
        response.setAssignmentTitle(dto.getAssignmentTitle());
        response.setAssignmentType(mapAssignmentResultType(dto.getAssignmentType()));
        response.setOrderNumber(dto.getOrderNumber());
        response.setMaxPoints(dto.getMaxPoints());
        response.setEarnedScore(dto.getEarnedScore());
        response.setIsCorrect(dto.getIsCorrect());
        response.setIsGraded(dto.getIsGraded());
        response.setTeacherFeedback(dto.getTeacherFeedback());

        if (dto.getAnswer() != null) {
            response.setAnswer(toApiAnswerResponse(dto.getAnswer()));
        }

        return response;
    }

    public SubmitAnswerRequestDto fromApiSubmitAnswerRequest(SubmitAnswerRequest request) {
        if (request == null) {
            return null;
        }

        return SubmitAnswerRequestDto.builder()
                .selectedOptionId(request.getSelectedOptionId())
                .selectedOptionIds(request.getSelectedOptionIds())
                .answerText(request.getAnswerText())
                .sourceCode(request.getSourceCode())
                .programmingLanguage(request.getProgrammingLanguage())
                .build();
    }

    private AnswerResponse.AssignmentTypeEnum mapAssignmentType(String type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case "SINGLE_CHOICE" -> AnswerResponse.AssignmentTypeEnum.SINGLE_CHOICE;
            case "MULTIPLE_CHOICE" -> AnswerResponse.AssignmentTypeEnum.MULTIPLE_CHOICE;
            case "OPEN_QUESTION" -> AnswerResponse.AssignmentTypeEnum.OPEN_QUESTION;
            case "CODING" -> AnswerResponse.AssignmentTypeEnum.CODING;
            default -> null;
        };
    }

    private AssignmentResultResponse.AssignmentTypeEnum mapAssignmentResultType(String type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case "SINGLE_CHOICE" -> AssignmentResultResponse.AssignmentTypeEnum.SINGLE_CHOICE;
            case "MULTIPLE_CHOICE" -> AssignmentResultResponse.AssignmentTypeEnum.MULTIPLE_CHOICE;
            case "OPEN_QUESTION" -> AssignmentResultResponse.AssignmentTypeEnum.OPEN_QUESTION;
            case "CODING" -> AssignmentResultResponse.AssignmentTypeEnum.CODING;
            default -> null;
        };
    }

    private AnswerResponse.CompilationStatusEnum mapCompilationStatus(String status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case "NOT_COMPILED" -> AnswerResponse.CompilationStatusEnum.NOT_COMPILED;
            case "SUCCESS" -> AnswerResponse.CompilationStatusEnum.SUCCESS;
            case "ERROR" -> AnswerResponse.CompilationStatusEnum.ERROR;
            case "TIMEOUT" -> AnswerResponse.CompilationStatusEnum.TIMEOUT;
            default -> null;
        };
    }

    private AnswerResponse.ExecutionStatusEnum mapExecutionStatus(String status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case "NOT_EXECUTED" -> AnswerResponse.ExecutionStatusEnum.NOT_EXECUTED;
            case "SUCCESS" -> AnswerResponse.ExecutionStatusEnum.SUCCESS;
            case "RUNTIME_ERROR" -> AnswerResponse.ExecutionStatusEnum.RUNTIME_ERROR;
            case "TIME_LIMIT_EXCEEDED" -> AnswerResponse.ExecutionStatusEnum.TIME_LIMIT_EXCEEDED;
            case "MEMORY_LIMIT_EXCEEDED" -> AnswerResponse.ExecutionStatusEnum.MEMORY_LIMIT_EXCEEDED;
            case "SYSTEM_ERROR" -> AnswerResponse.ExecutionStatusEnum.SYSTEM_ERROR;
            default -> null;
        };
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }
}
