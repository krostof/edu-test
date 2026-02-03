package com.edutest.persistance.repository;

import com.edutest.domain.assignment.*;
import com.edutest.domain.assignment.coding.CodingAssignment;
import com.edutest.domain.assignment.coding.TestCase;
import com.edutest.domain.assignment.common.ChoiceOption;
import com.edutest.domain.assignment.multiplechoice.MultipleChoiceAssignment;
import com.edutest.domain.assignment.openquestion.OpenQuestionAssignment;
import com.edutest.domain.assignment.singlechoice.SingleChoiceAssignment;
import com.edutest.domain.test.Test;
import com.edutest.persistance.entity.assigment.*;
import com.edutest.persistance.entity.assigment.coding.CodingAssignmentEntity;
import com.edutest.persistance.entity.assigment.coding.TestCaseEntity;
import com.edutest.persistance.entity.assigment.common.ChoiceOptionEntity;
import com.edutest.persistance.entity.assigment.multiplechoice.MultipleChoiceAssignmentEntity;
import com.edutest.persistance.entity.assigment.openquestion.OpenQuestionAssignmentEntityEntity;
import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAssignmentEntityEntity;
import com.edutest.persistance.entity.test.TestEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of domain repository that handles mapping between domain objects and JPA entities.
 */
@Repository
@RequiredArgsConstructor
@Transactional
public class AssignmentRepositoryImpl implements AssignmentRepository {

    private final AssignmentJpaRepository jpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Assignment save(Assignment assignment) {
        AssignmentEntity entity = mapToEntity(assignment);
        AssignmentEntity savedEntity = jpaRepository.save(entity);
        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<Assignment> findById(Long id) {
        return jpaRepository.findById(id)
                .map(this::mapToDomain);
    }

    @Override
    public List<Assignment> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Assignment> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable)
                .map(this::mapToDomain);
    }

    @Override
    public List<Assignment> findByTest(Test test) {
        TestEntity testEntity = entityManager.getReference(TestEntity.class, test.getId());
        return jpaRepository.findByTestEntity(testEntity).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Assignment> findByTestId(Long testId) {
        return jpaRepository.findByTestEntityIdOrderByOrderNumber(testId).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Assignment> findByTestOrderByOrderNumber(Test test) {
        TestEntity testEntity = entityManager.getReference(TestEntity.class, test.getId());
        return jpaRepository.findByTestEntityOrderByOrderNumber(testEntity).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Assignment> findByTestIdOrderByOrderNumber(Long testId) {
        return jpaRepository.findByTestEntityIdOrderByOrderNumber(testId).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Assignment> findByType(com.edutest.domain.assignment.AssignmentType type) {
        Class<? extends AssignmentEntity> entityClass = mapToEntityClass(type);
        return jpaRepository.findByType(entityClass).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Assignment> findByType(com.edutest.domain.assignment.AssignmentType type, Pageable pageable) {
        Class<? extends AssignmentEntity> entityClass = mapToEntityClass(type);
        return jpaRepository.findByType(entityClass, pageable)
                .map(this::mapToDomain);
    }

    @Override
    public List<Assignment> findByTestAndType(Test test, com.edutest.domain.assignment.AssignmentType type) {
        TestEntity testEntity = entityManager.getReference(TestEntity.class, test.getId());
        Class<? extends AssignmentEntity> entityClass = mapToEntityClass(type);
        return jpaRepository.findByTestEntity(testEntity).stream()
                .filter(entity -> entity.getClass().equals(entityClass))
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Assignment> findByTestIdAndType(Long testId, com.edutest.domain.assignment.AssignmentType type) {
        Class<? extends AssignmentEntity> entityClass = mapToEntityClass(type);
        return jpaRepository.findByTestEntityIdOrderByOrderNumber(testId).stream()
                .filter(entity -> entity.getClass().equals(entityClass))
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Assignment> findByTestIdAndOrderNumber(Long testId, Integer orderNumber) {
        return jpaRepository.findByTestIdAndOrderNumber(testId, orderNumber)
                .map(this::mapToDomain);
    }

    @Override
    public Page<Assignment> findByTitleOrDescriptionContaining(String searchTerm, Pageable pageable) {
        return jpaRepository.findByTitleOrDescriptionContaining(searchTerm, pageable)
                .map(this::mapToDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByTestIdAndOrderNumber(Long testId, Integer orderNumber) {
        return jpaRepository.existsByTestEntityIdAndOrderNumber(testId, orderNumber);
    }

    @Override
    public boolean existsByTestIdAndTitle(Long testId, String title) {
        return jpaRepository.existsByTestEntityIdAndTitle(testId, title);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByTestId(Long testId) {
        jpaRepository.deleteByTestEntityId(testId);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByTestId(Long testId) {
        return jpaRepository.countByTestEntityId(testId);
    }

    @Override
    public long countByType(com.edutest.domain.assignment.AssignmentType type) {
        Class<? extends AssignmentEntity> entityClass = mapToEntityClass(type);
        return jpaRepository.countByType(entityClass);
    }

    @Override
    public Float sumPointsByTestId(Long testId) {
        return jpaRepository.sumPointsByTestId(testId);
    }

    @Override
    public Integer getMaxOrderNumberByTestId(Long testId) {
        return jpaRepository.getMaxOrderNumberByTestId(testId);
    }

    // Mapping methods - Domain to Entity
    private AssignmentEntity mapToEntity(Assignment domain) {
        if (domain == null) {
            return null;
        }

        TestEntity testEntity = domain.getTestId() != null
            ? entityManager.getReference(TestEntity.class, domain.getTestId())
            : null;

        return switch (domain) {
            case SingleChoiceAssignment single -> mapSingleChoiceToEntity(single, testEntity);
            case MultipleChoiceAssignment multiple -> mapMultipleChoiceToEntity(multiple, testEntity);
            case OpenQuestionAssignment open -> mapOpenQuestionToEntity(open, testEntity);
            case CodingAssignment coding -> mapCodingToEntity(coding, testEntity);
            default -> throw new IllegalArgumentException("Unknown assignment type: " + domain.getClass().getName());
        };
    }

    private SingleChoiceAssignmentEntityEntity mapSingleChoiceToEntity(SingleChoiceAssignment domain, TestEntity testEntity) {
        SingleChoiceAssignmentEntityEntity entity = SingleChoiceAssignmentEntityEntity.builder()
                .randomizeOptions(domain.isRandomizeOptions())
                .build();

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
        MultipleChoiceAssignmentEntity entity = MultipleChoiceAssignmentEntity.builder()
                .randomizeOptions(domain.isRandomizeOptions())
                .partialScoring(domain.isPartialScoring())
                .penaltyForWrong(domain.isPenaltyForWrong())
                .build();

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
        OpenQuestionAssignmentEntityEntity entity = OpenQuestionAssignmentEntityEntity.builder()
                .maxLength(domain.getMaxLength())
                .minLength(domain.getMinLength())
                .sampleAnswer(domain.getSampleAnswer())
                .gradingRubric(domain.getGradingRubric())
                .allowHtml(domain.getAllowHtml())
                .caseSensitive(domain.getCaseSensitive())
                .build();

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

    private void mapCommonFields(Assignment domain, AssignmentEntity entity, TestEntity testEntity) {
        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setOrderNumber(domain.getOrderNumber());
        entity.setPoints(domain.getPoints() != null ? domain.getPoints().intValue() : null);
        entity.setTestEntity(testEntity);
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
    }

    private ChoiceOptionEntity mapChoiceOptionToEntity(ChoiceOption domain, AssignmentEntity assignmentEntity) {
        ChoiceOptionEntity entity = ChoiceOptionEntity.builder()
                .assignmentEntity(assignmentEntity)
                .optionText(domain.getOptionText())
                .isCorrect(domain.isCorrect())
                .orderNumber(domain.getOrderNumber())
                .explanation(domain.getExplanation())
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }

        return entity;
    }

    private TestCaseEntity mapTestCaseToEntity(TestCase domain, CodingAssignmentEntity assignmentEntity) {
        TestCaseEntity entity = TestCaseEntity.builder()
                .assignment(assignmentEntity)
                .inputData(domain.getInputData())
                .expectedOutput(domain.getExpectedOutput())
                .isPublic(domain.getIsPublic())
                .description(domain.getDescription())
                .weight(domain.getWeight())
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }

        return entity;
    }

    // Mapping methods - Entity to Domain
    private Assignment mapToDomain(AssignmentEntity entity) {
        if (entity == null) {
            return null;
        }

        return switch (entity) {
            case SingleChoiceAssignmentEntityEntity single -> mapSingleChoiceToDomain(single);
            case MultipleChoiceAssignmentEntity multiple -> mapMultipleChoiceToDomain(multiple);
            case OpenQuestionAssignmentEntityEntity open -> mapOpenQuestionToDomain(open);
            case CodingAssignmentEntity coding -> mapCodingToDomain(coding);
            default -> throw new IllegalArgumentException("Unknown entity type: " + entity.getClass().getName());
        };
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
                .options(options)
                .randomizeOptions(entity.getRandomizeOptions() != null ? entity.getRandomizeOptions() : false)
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
                .options(options)
                .randomizeOptions(entity.getRandomizeOptions() != null ? entity.getRandomizeOptions() : false)
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
                .correct(entity.isCorrectAnswer())
                .orderNumber(entity.getOrderNumber())
                .explanation(entity.getExplanation())
                .build();
    }

    private TestCase mapTestCaseToDomain(TestCaseEntity entity) {
        return TestCase.builder()
                .id(entity.getId())
                .inputData(entity.getInputData())
                .expectedOutput(entity.getExpectedOutput())
                .isPublic(entity.getIsPublic())
                .description(entity.getDescription())
                .weight(entity.getWeight())
                .build();
    }

    // Helper methods
    private Class<? extends AssignmentEntity> mapToEntityClass(com.edutest.domain.assignment.AssignmentType type) {
        return switch (type) {
            case SINGLE_CHOICE -> SingleChoiceAssignmentEntityEntity.class;
            case MULTIPLE_CHOICE -> MultipleChoiceAssignmentEntity.class;
            case OPEN_QUESTION -> OpenQuestionAssignmentEntityEntity.class;
            case CODING -> CodingAssignmentEntity.class;
        };
    }
}

