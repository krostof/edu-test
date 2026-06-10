package com.edutest.util;

import com.edutest.api.model.Assignment;
import com.edutest.api.model.AssignmentResponse;
import com.edutest.api.model.ChoiceOptionRequest;
import com.edutest.api.model.ChoiceOptionResponse;
import com.edutest.api.model.TestCaseRequest;
import com.edutest.api.model.TestCaseResponse;
import com.edutest.domain.assignment.AssignmentType;
import com.edutest.domain.assignment.coding.CodingAssignment;
import com.edutest.domain.assignment.coding.TestCase;
import com.edutest.domain.assignment.common.ChoiceOption;
import com.edutest.domain.assignment.multiplechoice.MultipleChoiceAssignment;
import com.edutest.domain.assignment.openquestion.OpenQuestionAssignment;
import com.edutest.domain.assignment.singlechoice.SingleChoiceAssignment;
import com.edutest.dto.ChoiceOptionDto;
import com.edutest.dto.TestCaseDto;
import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.assigment.coding.CodingAssignmentEntity;
import com.edutest.persistance.entity.assigment.coding.TestCaseEntity;
import com.edutest.persistance.entity.assigment.common.ChoiceOptionEntity;
import com.edutest.persistance.entity.assigment.multiplechoice.MultipleChoiceAssignmentEntity;
import com.edutest.persistance.entity.assigment.openquestion.OpenQuestionAssignmentEntityEntity;
import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAssignmentEntityEntity;
import com.edutest.persistance.entity.test.TestEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AssignmentMapper {

    /**
     * Maps domain Assignment to API AssignmentResponse model
     */
    public AssignmentResponse toApiResponse(com.edutest.domain.assignment.Assignment assignment) {
        AssignmentResponse resp = new AssignmentResponse();
        resp.setId(assignment.getId());
        resp.setType(assignment.getType().name());
        resp.setTitle(assignment.getTitle());
        resp.setDescription(assignment.getDescription());
        resp.setOrderNumber(assignment.getOrderNumber());
        resp.setPoints(assignment.getPoints());
        resp.setIsAttachmentAllowed(assignment.getIsAttachmentAllowed());

        if (assignment instanceof SingleChoiceAssignment sc) {
            resp.setOptions(mapOptionsToApi(sc.getOptions()));
            resp.setRandomizeOptions(sc.isRandomizeOptions());
        } else if (assignment instanceof MultipleChoiceAssignment mc) {
            resp.setOptions(mapOptionsToApi(mc.getOptions()));
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
            resp.setTestCases(mapTestCasesToApi(ca.getTestCases()));
        }

        return resp;
    }

    /**
     * Maps API ChoiceOptionRequest to domain ChoiceOption
     */
    public ChoiceOption toChoiceOption(ChoiceOptionRequest dto) {
        return ChoiceOption.builder()
                .optionText(dto.getOptionText())
                .correct(Boolean.TRUE.equals(dto.getCorrect()))
                .orderNumber(dto.getOrderNumber())
                .explanation(dto.getExplanation())
                .build();
    }

    /**
     * Maps API TestCaseRequest to domain TestCase
     */
    public TestCase toTestCase(TestCaseRequest dto) {
        return TestCase.builder()
                .inputData(dto.getInputData())
                .expectedOutput(dto.getExpectedOutput())
                .isPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false)
                .description(dto.getDescription())
                .weight(dto.getWeight() != null ? dto.getWeight().floatValue() : null)
                .build();
    }

    private List<ChoiceOptionResponse> mapOptionsToApi(List<ChoiceOption> options) {
        if (options == null) return null;
        return options.stream().map(this::toChoiceOptionResponse).collect(Collectors.toList());
    }

    private ChoiceOptionResponse toChoiceOptionResponse(ChoiceOption option) {
        ChoiceOptionResponse resp = new ChoiceOptionResponse();
        resp.setId(option.getId());
        resp.setOptionText(option.getOptionText());
        resp.setCorrect(option.isCorrect());
        resp.setOrderNumber(option.getOrderNumber());
        resp.setExplanation(option.getExplanation());
        return resp;
    }

    private List<TestCaseResponse> mapTestCasesToApi(List<TestCase> testCases) {
        if (testCases == null) return null;
        return testCases.stream().map(this::toTestCaseResponse).collect(Collectors.toList());
    }

    private TestCaseResponse toTestCaseResponse(TestCase tc) {
        TestCaseResponse resp = new TestCaseResponse();
        resp.setId(tc.getId());
        resp.setInputData(tc.getInputData());
        resp.setExpectedOutput(tc.getExpectedOutput());
        resp.setIsPublic(tc.getIsPublic());
        resp.setDescription(tc.getDescription());
        resp.setWeight(tc.getWeight() != null ? tc.getWeight().intValue() : null);
        return resp;
    }

    // =====================================================
    // Legacy methods using internal DTOs (for backwards compatibility)
    // =====================================================

    /**
     * @deprecated Use toApiResponse instead
     */
    @Deprecated
    public com.edutest.dto.AssignmentResponse toResponse(com.edutest.domain.assignment.Assignment assignment) {
        com.edutest.dto.AssignmentResponse resp = new com.edutest.dto.AssignmentResponse();
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

    /**
     * @deprecated Use toChoiceOption(ChoiceOptionRequest) instead
     */
    @Deprecated
    public ChoiceOption toChoiceOption(ChoiceOptionDto dto) {
        return ChoiceOption.builder()
                .optionText(dto.getOptionText())
                .correct(dto.isCorrect())
                .orderNumber(dto.getOrderNumber())
                .explanation(dto.getExplanation())
                .build();
    }

    /**
     * @deprecated Use toTestCase(TestCaseRequest) instead
     */
    @Deprecated
    public TestCase toTestCase(TestCaseDto dto) {
        return TestCase.builder()
                .inputData(dto.getInputData())
                .expectedOutput(dto.getExpectedOutput())
                .isPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false)
                .description(dto.getDescription())
                .weight(dto.getWeight() != null ? dto.getWeight().floatValue() : 1f)
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
        dto.setWeight(tc.getWeight() != null ? tc.getWeight().intValue() : null);
        return dto;
    }

    // =====================================================
    // Entity <-> Domain mapping
    // =====================================================

    public AssignmentEntity toEntity(com.edutest.domain.assignment.Assignment domain, TestEntity testEntity) {
        if (domain == null) {
            return null;
        }

        return switch (domain) {
            case SingleChoiceAssignment single -> mapSingleChoiceToEntity(single, testEntity);
            case MultipleChoiceAssignment multiple -> mapMultipleChoiceToEntity(multiple, testEntity);
            case OpenQuestionAssignment open -> mapOpenQuestionToEntity(open, testEntity);
            case CodingAssignment coding -> mapCodingToEntity(coding, testEntity);
            default -> throw new IllegalArgumentException("Unknown assignment type: " + domain.getClass().getName());
        };
    }

    public com.edutest.domain.assignment.Assignment toDomain(AssignmentEntity entity) {
        if (entity == null) {
            return null;
        }

        initializeLazyCollections(entity);

        return switch (entity) {
            case SingleChoiceAssignmentEntityEntity single -> mapSingleChoiceToDomain(single);
            case MultipleChoiceAssignmentEntity multiple -> mapMultipleChoiceToDomain(multiple);
            case OpenQuestionAssignmentEntityEntity open -> mapOpenQuestionToDomain(open);
            case CodingAssignmentEntity coding -> mapCodingToDomain(coding);
            default -> throw new IllegalArgumentException("Unknown entity type: " + entity.getClass().getName());
        };
    }

    public Class<? extends AssignmentEntity> entityClassFor(AssignmentType type) {
        return switch (type) {
            case SINGLE_CHOICE -> SingleChoiceAssignmentEntityEntity.class;
            case MULTIPLE_CHOICE -> MultipleChoiceAssignmentEntity.class;
            case OPEN_QUESTION -> OpenQuestionAssignmentEntityEntity.class;
            case CODING -> CodingAssignmentEntity.class;
        };
    }

    private void initializeLazyCollections(AssignmentEntity entity) {
        if (entity instanceof SingleChoiceAssignmentEntityEntity single) {
            single.getOptions().size();
        } else if (entity instanceof MultipleChoiceAssignmentEntity multiple) {
            multiple.getOptions().size();
        } else if (entity instanceof CodingAssignmentEntity coding) {
            coding.getTestCases().size();
        }
    }

    private SingleChoiceAssignmentEntityEntity mapSingleChoiceToEntity(SingleChoiceAssignment domain, TestEntity testEntity) {
        SingleChoiceAssignmentEntityEntity entity = new SingleChoiceAssignmentEntityEntity();
        entity.setRandomizeOptions(domain.isRandomizeOptions());

        mapCommonFields(domain, entity, testEntity);

        if (domain.getOptions() != null) {
            List<ChoiceOptionEntity> optionEntities = domain.getOptions().stream()
                    .map(option -> mapChoiceOptionToEntity(option, entity))
                    .collect(Collectors.toList());
            entity.setOptions(optionEntities);
        }

        return entity;
    }

    private MultipleChoiceAssignmentEntity mapMultipleChoiceToEntity(MultipleChoiceAssignment domain, TestEntity testEntity) {
        MultipleChoiceAssignmentEntity entity = new MultipleChoiceAssignmentEntity();
        entity.setRandomizeOptions(domain.isRandomizeOptions());
        entity.setPartialScoring(domain.isPartialScoring());
        entity.setPenaltyForWrong(domain.isPenaltyForWrong());

        mapCommonFields(domain, entity, testEntity);

        if (domain.getOptions() != null) {
            List<ChoiceOptionEntity> optionEntities = domain.getOptions().stream()
                    .map(option -> mapChoiceOptionToEntity(option, entity))
                    .collect(Collectors.toList());
            entity.setOptions(optionEntities);
        }

        return entity;
    }

    private OpenQuestionAssignmentEntityEntity mapOpenQuestionToEntity(OpenQuestionAssignment domain, TestEntity testEntity) {
        OpenQuestionAssignmentEntityEntity entity = new OpenQuestionAssignmentEntityEntity();
        entity.setMaxLength(domain.getMaxLength());
        entity.setMinLength(domain.getMinLength());
        entity.setSampleAnswer(domain.getSampleAnswer());
        entity.setGradingRubric(domain.getGradingRubric());
        entity.setAllowHtml(domain.getAllowHtml());
        entity.setCaseSensitive(domain.getCaseSensitive());

        mapCommonFields(domain, entity, testEntity);

        return entity;
    }

    private CodingAssignmentEntity mapCodingToEntity(CodingAssignment domain, TestEntity testEntity) {
        CodingAssignmentEntity entity = new CodingAssignmentEntity();
        entity.setTimeLimitMs(domain.getTimeLimitMs());
        entity.setMemoryLimitMb(domain.getMemoryLimitMb());
        entity.setAllowedLanguagesStr(domain.getAllowedLanguages());
        entity.setStarterCode(domain.getStarterCode());
        entity.setSolutionTemplate(domain.getSolutionTemplate());

        mapCommonFields(domain, entity, testEntity);

        if (domain.getTestCases() != null) {
            List<TestCaseEntity> testCaseEntities = domain.getTestCases().stream()
                    .map(testCase -> mapTestCaseToEntity(testCase, entity))
                    .collect(Collectors.toList());
            entity.setTestCases(testCaseEntities);
        }

        return entity;
    }

    private void mapCommonFields(com.edutest.domain.assignment.Assignment domain, AssignmentEntity entity, TestEntity testEntity) {
        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setOrderNumber(domain.getOrderNumber());
        entity.setPoints(domain.getPoints() != null ? domain.getPoints().intValue() : null);
        entity.setTestEntity(testEntity);
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
    }

    private ChoiceOptionEntity mapChoiceOptionToEntity(ChoiceOption domain, AssignmentEntity assignmentEntity) {
        ChoiceOptionEntity entity = new ChoiceOptionEntity();
        entity.setAssignmentEntity(assignmentEntity);
        entity.setOptionText(domain.getOptionText());
        entity.setIsCorrect(domain.isCorrect());
        entity.setOrderNumber(domain.getOrderNumber());
        entity.setExplanation(domain.getExplanation());
        entity.setVersion(domain.getVersion());

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }

        return entity;
    }

    private TestCaseEntity mapTestCaseToEntity(TestCase domain, CodingAssignmentEntity assignmentEntity) {
        TestCaseEntity entity = new TestCaseEntity();
        entity.setAssignment(assignmentEntity);
        entity.setInputData(domain.getInputData());
        entity.setExpectedOutput(domain.getExpectedOutput());
        entity.setIsPublic(domain.getIsPublic());
        entity.setDescription(domain.getDescription());
        entity.setWeight(domain.getWeight() != null ? domain.getWeight().intValue() : null);
        entity.setVersion(domain.getVersion());

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }

        return entity;
    }

    private SingleChoiceAssignment mapSingleChoiceToDomain(SingleChoiceAssignmentEntityEntity entity) {
        List<ChoiceOption> options = entity.getOptions() != null
                ? entity.getOptions().stream()
                        .map(this::mapChoiceOptionToDomain)
                        .collect(Collectors.toList())
                : List.of();

        return SingleChoiceAssignment.builder()
                .id(entity.getId())
                .testId(entity.getTestEntity() != null ? entity.getTestEntity().getId() : null)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .orderNumber(entity.getOrderNumber())
                .points(entity.getPoints() != null ? entity.getPoints().floatValue() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .randomizeOptions(entity.getRandomizeOptions() != null ? entity.getRandomizeOptions() : false)
                .options(options)
                .build();
    }

    private MultipleChoiceAssignment mapMultipleChoiceToDomain(MultipleChoiceAssignmentEntity entity) {
        List<ChoiceOption> options = entity.getOptions() != null
                ? entity.getOptions().stream()
                        .map(this::mapChoiceOptionToDomain)
                        .collect(Collectors.toList())
                : List.of();

        return MultipleChoiceAssignment.builder()
                .id(entity.getId())
                .testId(entity.getTestEntity() != null ? entity.getTestEntity().getId() : null)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .orderNumber(entity.getOrderNumber())
                .points(entity.getPoints() != null ? entity.getPoints().floatValue() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .options(options)
                .partialScoring(entity.getPartialScoring() != null ? entity.getPartialScoring() : false)
                .penaltyForWrong(entity.getPenaltyForWrong() != null ? entity.getPenaltyForWrong() : false)
                .build();
    }

    private OpenQuestionAssignment mapOpenQuestionToDomain(OpenQuestionAssignmentEntityEntity entity) {
        return OpenQuestionAssignment.builder()
                .id(entity.getId())
                .testId(entity.getTestEntity() != null ? entity.getTestEntity().getId() : null)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .orderNumber(entity.getOrderNumber())
                .points(entity.getPoints() != null ? entity.getPoints().floatValue() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .maxLength(entity.getMaxLength())
                .minLength(entity.getMinLength())
                .sampleAnswer(entity.getSampleAnswer())
                .gradingRubric(entity.getGradingRubric())
                .allowHtml(entity.getAllowHtml())
                .caseSensitive(entity.getCaseSensitive())
                .build();
    }

    private CodingAssignment mapCodingToDomain(CodingAssignmentEntity entity) {
        List<TestCase> testCases = entity.getTestCases() != null
                ? entity.getTestCases().stream()
                        .map(this::mapTestCaseToDomain)
                        .collect(Collectors.toList())
                : List.of();

        return CodingAssignment.builder()
                .id(entity.getId())
                .testId(entity.getTestEntity() != null ? entity.getTestEntity().getId() : null)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .orderNumber(entity.getOrderNumber())
                .points(entity.getPoints() != null ? entity.getPoints().floatValue() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .timeLimitMs(entity.getTimeLimitMs())
                .memoryLimitMb(entity.getMemoryLimitMb())
                .allowedLanguages(entity.getAllowedLanguagesStr())
                .starterCode(entity.getStarterCode())
                .solutionTemplate(entity.getSolutionTemplate())
                .testCases(testCases)
                .build();
    }

    private ChoiceOption mapChoiceOptionToDomain(ChoiceOptionEntity entity) {
        return ChoiceOption.builder()
                .id(entity.getId())
                .assignmentId(entity.getAssignmentEntity() != null ? entity.getAssignmentEntity().getId() : null)
                .optionText(entity.getOptionText())
                .correct(entity.getIsCorrect() != null ? entity.getIsCorrect() : false)
                .orderNumber(entity.getOrderNumber())
                .explanation(entity.getExplanation())
                .version(entity.getVersion())
                .build();
    }

    private TestCase mapTestCaseToDomain(TestCaseEntity entity) {
        return TestCase.builder()
                .id(entity.getId())
                .inputData(entity.getInputData())
                .expectedOutput(entity.getExpectedOutput())
                .isPublic(entity.getIsPublic() != null ? entity.getIsPublic() : false)
                .description(entity.getDescription())
                .weight(entity.getWeight() != null ? entity.getWeight().floatValue() : null)
                .version(entity.getVersion())
                .build();
    }
}
