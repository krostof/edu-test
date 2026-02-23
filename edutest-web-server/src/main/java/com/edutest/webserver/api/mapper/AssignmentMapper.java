package com.edutest.webserver.api.mapper;

import com.edutest.api.model.Assignment;
import com.edutest.domain.assignment.coding.CodingAssignment;
import com.edutest.domain.assignment.coding.TestCase;
import com.edutest.domain.assignment.common.ChoiceOption;
import com.edutest.domain.assignment.multiplechoice.MultipleChoiceAssignment;
import com.edutest.domain.assignment.openquestion.OpenQuestionAssignment;
import com.edutest.domain.assignment.singlechoice.SingleChoiceAssignment;
import com.edutest.webserver.api.dto.AssignmentResponse;
import com.edutest.webserver.api.dto.ChoiceOptionDto;
import com.edutest.webserver.api.dto.TestCaseDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AssignmentMapper {

    public AssignmentResponse toResponse(com.edutest.domain.assignment.Assignment assignment) {
        AssignmentResponse resp = new AssignmentResponse();
        resp.setId(assignment.getId());
        resp.setType(assignment.getType().name());
        resp.setTitle(assignment.getTitle());
        resp.setDescription(assignment.getDescription());
        resp.setOrderNumber(assignment.getOrderNumber());
        resp.setPoints(assignment.getPoints());
        resp.setIsAttachmentAllowed(assignment.getIsAttachmentAllowed());

        if (assignment instanceof SingleChoiceAssignment sc) {
            resp.setOptions(mapOptions(sc.getOptions()));
            resp.setRandomizeOptions(sc.isRandomizeOptions());
        } else if (assignment instanceof MultipleChoiceAssignment mc) {
            resp.setOptions(mapOptions(mc.getOptions()));
            resp.setRandomizeOptions(mc.isRandomizeOptions());
            resp.setPartialScoring(mc.isPartialScoring());
            resp.setPenaltyForWrong(mc.isPenaltyForWrong());
        } else if (assignment instanceof OpenQuestionAssignment oq) {
            resp.setMinLength(oq.getMinLength());
            resp.setMaxLength(oq.getMaxLength());
            resp.setSampleAnswer(oq.getSampleAnswer());
            resp.setGradingRubric(oq.getGradingRubric());
            resp.setAllowHtml(oq.getAllowHtml());
        } else if (assignment instanceof CodingAssignment ca) {
            resp.setTimeLimitMs(ca.getTimeLimitMs());
            resp.setMemoryLimitMb(ca.getMemoryLimitMb());
            resp.setAllowedLanguages(ca.getAllowedLanguages());
            resp.setStarterCode(ca.getStarterCode());
            resp.setSolutionTemplate(ca.getSolutionTemplate());
            resp.setTestCases(mapTestCases(ca.getTestCases()));
        }

        return resp;
    }

    public Assignment toApiAssignment(com.edutest.domain.assignment.Assignment assignment) {
        Assignment api = new Assignment();
        api.setId(assignment.getId());
        api.setType(assignment.getType().name());
        api.setTitle(assignment.getTitle());
        api.setDescription(assignment.getDescription());
        api.setOrderNumber(assignment.getOrderNumber());
        if (assignment.getPoints() != null) {
            api.setPoints(assignment.getPoints().intValue());
        }
        return api;
    }

    public ChoiceOption toChoiceOption(ChoiceOptionDto dto) {
        return ChoiceOption.builder()
                .optionText(dto.getOptionText())
                .correct(dto.isCorrect())
                .orderNumber(dto.getOrderNumber())
                .explanation(dto.getExplanation())
                .build();
    }

    public TestCase toTestCase(TestCaseDto dto) {
        return TestCase.builder()
                .inputData(dto.getInputData())
                .expectedOutput(dto.getExpectedOutput())
                .isPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false)
                .description(dto.getDescription())
                .weight(dto.getWeight() != null ? dto.getWeight() : 1)
                .build();
    }

    private List<ChoiceOptionDto> mapOptions(List<ChoiceOption> options) {
        if (options == null) return null;
        return options.stream().map(this::toChoiceOptionDto).collect(Collectors.toList());
    }

    private ChoiceOptionDto toChoiceOptionDto(ChoiceOption option) {
        ChoiceOptionDto dto = new ChoiceOptionDto();
        dto.setId(option.getId());
        dto.setOptionText(option.getOptionText());
        dto.setCorrect(option.isCorrect());
        dto.setOrderNumber(option.getOrderNumber());
        dto.setExplanation(option.getExplanation());
        return dto;
    }

    private List<TestCaseDto> mapTestCases(List<TestCase> testCases) {
        if (testCases == null) return null;
        return testCases.stream().map(this::toTestCaseDto).collect(Collectors.toList());
    }

    private TestCaseDto toTestCaseDto(TestCase tc) {
        TestCaseDto dto = new TestCaseDto();
        dto.setId(tc.getId());
        dto.setInputData(tc.getInputData());
        dto.setExpectedOutput(tc.getExpectedOutput());
        dto.setIsPublic(tc.getIsPublic());
        dto.setDescription(tc.getDescription());
        dto.setWeight(tc.getWeight());
        return dto;
    }
}
