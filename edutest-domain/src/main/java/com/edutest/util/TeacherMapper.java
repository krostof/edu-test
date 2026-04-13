package com.edutest.util;

import com.edutest.api.model.*;
import com.edutest.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TeacherMapper {

    public AttemptPageResponse toApiAttemptPageResponse(Page<AttemptListItemDto> page) {
        AttemptPageResponse response = new AttemptPageResponse();
        response.setContent(page.getContent().stream()
                .map(this::toApiAttemptListItem)
                .collect(Collectors.toList()));
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setSize(page.getSize());
        response.setNumber(page.getNumber());
        response.setNumberOfElements(page.getNumberOfElements());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        return response;
    }

    public AttemptListItem toApiAttemptListItem(AttemptListItemDto dto) {
        if (dto == null) {
            return null;
        }

        AttemptListItem item = new AttemptListItem();
        item.setAttemptId(dto.getAttemptId());
        item.setStudentId(dto.getStudentId());
        item.setStudentName(dto.getStudentName());
        item.setStudentEmail(dto.getStudentEmail());
        item.setGroupId(dto.getGroupId());
        item.setGroupName(dto.getGroupName());
        item.setStartedAt(toOffsetDateTime(dto.getStartedAt()));
        item.setFinishedAt(toOffsetDateTime(dto.getFinishedAt()));
        item.setScore(dto.getScore());
        item.setMaxPossibleScore(dto.getMaxPossibleScore());
        item.setScorePercentage(dto.getScorePercentage());
        item.setStatus(mapStatus(dto.getStatus()));
        item.setPendingGradingCount(dto.getPendingGradingCount());
        return item;
    }

    public TestStatsSummary toApiTestStatsSummary(TestStatsSummaryDto dto) {
        if (dto == null) {
            return null;
        }

        TestStatsSummary summary = new TestStatsSummary();
        summary.setTestId(dto.getTestId());
        summary.setTestTitle(dto.getTestTitle());
        summary.setTotalAttempts(dto.getTotalAttempts());
        summary.setCompletedAttempts(dto.getCompletedAttempts());
        summary.setInProgressAttempts(dto.getInProgressAttempts());
        summary.setGradedAttempts(dto.getGradedAttempts());
        summary.setAverageScore(dto.getAverageScore());
        summary.setMedianScore(dto.getMedianScore());
        summary.setMinScore(dto.getMinScore());
        summary.setMaxScore(dto.getMaxScore());
        summary.setAverageScorePercentage(dto.getAverageScorePercentage());

        if (dto.getScoreDistribution() != null) {
            summary.setScoreDistribution(dto.getScoreDistribution().stream()
                    .map(this::toApiScoreDistributionItem)
                    .collect(Collectors.toList()));
        }

        return summary;
    }

    public ScoreDistributionItem toApiScoreDistributionItem(ScoreDistributionItemDto dto) {
        if (dto == null) {
            return null;
        }

        ScoreDistributionItem item = new ScoreDistributionItem();
        item.setRangeLabel(dto.getRangeLabel());
        item.setCount(dto.getCount());
        item.setPercentage(dto.getPercentage());
        return item;
    }

    public AnswerReviewResponse toApiAnswerReviewResponse(AnswerReviewDto dto) {
        if (dto == null) {
            return null;
        }

        AnswerReviewResponse response = new AnswerReviewResponse();
        response.setAnswerId(dto.getAnswerId());
        response.setAttemptId(dto.getAttemptId());
        response.setAssignmentId(dto.getAssignmentId());
        response.setAssignmentTitle(dto.getAssignmentTitle());
        response.setAssignmentType(mapReviewAssignmentType(dto.getAssignmentType()));
        response.setAssignmentDescription(dto.getAssignmentDescription());
        response.setMaxPoints(dto.getMaxPoints());
        response.setStudentId(dto.getStudentId());
        response.setStudentName(dto.getStudentName());
        response.setAnsweredAt(toOffsetDateTime(dto.getAnsweredAt()));

        // Answer content
        response.setAnswerText(dto.getAnswerText());
        response.setSelectedOptionId(dto.getSelectedOptionId());
        response.setSelectedOptionIds(dto.getSelectedOptionIds());
        response.setSourceCode(dto.getSourceCode());
        response.setProgrammingLanguage(dto.getProgrammingLanguage());

        // Choice question info
        response.setCorrectOptionIds(dto.getCorrectOptionIds());
        if (dto.getOptions() != null) {
            response.setOptions(dto.getOptions().stream()
                    .map(this::toApiChoiceOption)
                    .collect(Collectors.toList()));
        }

        // Open question info
        response.setSampleAnswer(dto.getSampleAnswer());
        response.setGradingRubric(dto.getGradingRubric());

        // Grading info
        response.setScore(dto.getScore());
        response.setIsGraded(dto.getIsGraded());
        response.setTeacherFeedback(dto.getTeacherFeedback());

        return response;
    }

    public GradeAnswerRequestDto fromApiGradeAnswerRequest(GradeAnswerRequest request) {
        if (request == null) {
            return null;
        }

        return GradeAnswerRequestDto.builder()
                .score(request.getScore())
                .feedback(request.getFeedback())
                .build();
    }

    private ChoiceOptionResponse toApiChoiceOption(ChoiceOptionDto dto) {
        if (dto == null) {
            return null;
        }

        ChoiceOptionResponse opt = new ChoiceOptionResponse();
        opt.setId(dto.getId());
        opt.setOptionText(dto.getOptionText());
        opt.setCorrect(dto.isCorrect());
        opt.setOrderNumber(dto.getOrderNumber());
        opt.setExplanation(dto.getExplanation());
        return opt;
    }

    private AttemptListItem.StatusEnum mapStatus(String status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case "IN_PROGRESS" -> AttemptListItem.StatusEnum.IN_PROGRESS;
            case "SUBMITTED" -> AttemptListItem.StatusEnum.SUBMITTED;
            case "GRADED" -> AttemptListItem.StatusEnum.GRADED;
            default -> null;
        };
    }

    private AnswerReviewResponse.AssignmentTypeEnum mapReviewAssignmentType(String type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case "SINGLE_CHOICE" -> AnswerReviewResponse.AssignmentTypeEnum.SINGLE_CHOICE;
            case "MULTIPLE_CHOICE" -> AnswerReviewResponse.AssignmentTypeEnum.MULTIPLE_CHOICE;
            case "OPEN_QUESTION" -> AnswerReviewResponse.AssignmentTypeEnum.OPEN_QUESTION;
            case "CODING" -> AnswerReviewResponse.AssignmentTypeEnum.CODING;
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
