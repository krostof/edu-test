package com.edutest.service.assignmentservice;

import com.edutest.domain.assignment.*;
import com.edutest.domain.assignment.coding.CodingAssignment;
import com.edutest.domain.assignment.coding.TestCase;
import com.edutest.domain.assignment.multiplechoice.MultipleChoiceAssignment;
import com.edutest.domain.assignment.singlechoice.SingleChoiceAssignment;
import com.edutest.domain.assignment.openquestion.OpenQuestionAssignment;
import com.edutest.domain.assignment.common.ChoiceOption;
import com.edutest.domain.test.Test;
import com.edutest.persistance.repository.AssignmentRepository;
import com.edutest.persistance.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final TestRepository testRepository;

    public SingleChoiceAssignment createSingleChoiceAssignment(Long testId, String title, String description, 
                                                             Float points, List<ChoiceOption> options) {
        log.info("Creating single choice assignment: title={}, testId={}", title, testId);

        Test test = getTestById(testId);
        validateAssignmentCreation(test, title);

        Integer orderNumber = getNextOrderNumber(testId);

        SingleChoiceAssignment assignment = SingleChoiceAssignment.singleChoiceBuilder()
                .testId(testId)
                .title(title)
                .description(description)
                .points(points)
                .orderNumber(orderNumber)
                .options(options)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validateAssignment(assignment);
        Assignment savedAssignment = assignmentRepository.save(assignment);
        
        log.info("Single choice assignment created with id={}", savedAssignment.getId());
        return (SingleChoiceAssignment) savedAssignment;
    }

    public MultipleChoiceAssignment createMultipleChoiceAssignment(Long testId, String title, String description,
                                                                 Float points, List<ChoiceOption> options,
                                                                 boolean randomizeOptions, boolean partialScoring,
                                                                 boolean penaltyForWrong) {
        log.info("Creating multiple choice assignment: title={}, testId={}", title, testId);

        Test test = getTestById(testId);
        validateAssignmentCreation(test, title);

        Integer orderNumber = getNextOrderNumber(testId);

        MultipleChoiceAssignment assignment = MultipleChoiceAssignment.multipleChoiceBuilder()
                .testId(testId)
                .title(title)
                .description(description)
                .points(points)
                .orderNumber(orderNumber)
                .options(options)
                .randomizeOptions(randomizeOptions)
                .partialScoring(partialScoring)
                .penaltyForWrong(penaltyForWrong)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validateAssignment(assignment);
        Assignment savedAssignment = assignmentRepository.save(assignment);
        
        log.info("Multiple choice assignment created with id={}", savedAssignment.getId());
        return (MultipleChoiceAssignment) savedAssignment;
    }

    public OpenQuestionAssignment createOpenQuestionAssignment(Long testId, String title, String description,
                                                             Float points, Integer maxLength, Integer minLength,
                                                             boolean attachmentsAllowed) {
        log.info("Creating open question assignment: title={}, testId={}", title, testId);

        Test test = getTestById(testId);
        validateAssignmentCreation(test, title);

        Integer orderNumber = getNextOrderNumber(testId);

        OpenQuestionAssignment assignment = (OpenQuestionAssignment) OpenQuestionAssignment.builder()
                .testId(testId)
                .title(title)
                .description(description)
                .points(points)
                .isAttachmentAllowed(attachmentsAllowed)
                .orderNumber(orderNumber)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validateAssignment(assignment);
        Assignment savedAssignment = assignmentRepository.save(assignment);
        
        log.info("Open question assignment created with id={}", savedAssignment.getId());
        return (OpenQuestionAssignment) savedAssignment;
    }

    public CodingAssignment createCodingAssignment(Long testId, String title, String description, Float points,
                                                 Integer timeLimitMs, Integer memoryLimitMb, String allowedLanguages,
                                                 String starterCode, String solutionTemplate, List<TestCase> testCases) {
        log.info("Creating coding assignment: title={}, testId={}", title, testId);

        Test test = getTestById(testId);
        validateAssignmentCreation(test, title);

        Integer orderNumber = getNextOrderNumber(testId);

        CodingAssignment assignment = CodingAssignment.codingBuilder()
                .testId(testId)
                .title(title)
                .description(description)
                .points(points)
                .orderNumber(orderNumber)
                .timeLimitMs(timeLimitMs)
                .memoryLimitMb(memoryLimitMb)
                .allowedLanguages(allowedLanguages)
                .starterCode(starterCode)
                .solutionTemplate(solutionTemplate)
                .testCases(testCases)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validateAssignment(assignment);
        Assignment savedAssignment = assignmentRepository.save(assignment);
        
        log.info("Coding assignment created with id={}", savedAssignment.getId());
        return (CodingAssignment) savedAssignment;
    }

    @Transactional(readOnly = true)
    public Assignment findById(Long id) {
        log.debug("Finding assignment by id: {}", id);
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Assignment> findByTestId(Long testId) {
        log.debug("Finding assignments by test id: {}", testId);
        return assignmentRepository.findByTestIdOrderByOrderNumber(testId);
    }

    @Transactional(readOnly = true)
    public List<Assignment> findByTestIdAndType(Long testId, AssignmentType type) {
        log.debug("Finding assignments by test id: {} and type: {}", testId, type);
        return assignmentRepository.findByTestIdAndType(testId, type);
    }

    @Transactional(readOnly = true)
    public Page<Assignment> findByType(AssignmentType type, Pageable pageable) {
        log.debug("Finding assignments by type: {} with pagination", type);
        return assignmentRepository.findByType(type, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Assignment> searchAssignments(String searchTerm, Pageable pageable) {
        log.debug("Searching assignments with term: {}", searchTerm);
        return assignmentRepository.findByTitleOrDescriptionContaining(searchTerm, pageable);
    }

    public Assignment updateAssignment(Long id, String title, String description, Float points) {
        log.info("Updating assignment: id={}", id);

        Assignment assignment = findById(id);

        if (title != null && !title.equals(assignment.getTitle())) {
            if (assignmentRepository.existsByTestIdAndTitle(assignment.getTestId(), title)) {
                throw new IllegalArgumentException("Assignment with title '" + title + "' already exists in this test");
            }
            assignment.updateTitle(title);
        }

        if (description != null) {
            assignment.updateDescription(description);
        }

        if (points != null) {
            assignment.updatePoints(points);
        }

        validateAssignment(assignment);
        Assignment updatedAssignment = assignmentRepository.save(assignment);
        
        log.info("Assignment updated successfully with id={}", updatedAssignment.getId());
        return updatedAssignment;
    }

    public void deleteAssignment(Long id) {
        log.info("Deleting assignment with id: {}", id);

        Assignment assignment = findById(id);
        
        Test test = getTestById(assignment.getTestId());
        if (test.isActive()) {
            throw new IllegalStateException("Cannot delete assignments from active tests");
        }

        assignmentRepository.deleteById(id);
        log.info("Assignment deleted successfully with id: {}", id);
    }

    public Assignment moveAssignment(Long id, Integer newOrderNumber) {
        log.info("Moving assignment {} to order {}", id, newOrderNumber);

        Assignment assignment = findById(id);
        
        if (assignmentRepository.existsByTestIdAndOrderNumber(assignment.getTestId(), newOrderNumber)) {
            throw new IllegalArgumentException("Order number " + newOrderNumber + " is already taken in this test");
        }

        assignment.setOrderNumber(newOrderNumber);
        assignment.setUpdatedAt(LocalDateTime.now());

        Assignment updatedAssignment = assignmentRepository.save(assignment);
        log.info("Assignment moved successfully");
        
        return updatedAssignment;
    }

    public Assignment duplicateAssignment(Long id) {
        log.info("Duplicating assignment with id: {}", id);

        Assignment original = findById(id);
        Integer newOrderNumber = getNextOrderNumber(original.getTestId());
        
        Assignment duplicate = createDuplicateAssignment(original, newOrderNumber);
        Assignment savedDuplicate = assignmentRepository.save(duplicate);
        
        log.info("Assignment duplicated successfully with new id={}", savedDuplicate.getId());
        return savedDuplicate;
    }

    @Transactional(readOnly = true)
    public ValidationResult validateAnswer(Long assignmentId, String answer) {
        log.debug("Validating answer for assignment: {}", assignmentId);
        Assignment assignment = findById(assignmentId);
        return assignment.validateAnswer(answer);
    }

    @Transactional(readOnly = true)
    public Float calculateScore(Long assignmentId, String answer) {
        log.debug("Calculating score for assignment: {}", assignmentId);
        Assignment assignment = findById(assignmentId);
        return assignment.calculateScore(answer);
    }

    @Transactional(readOnly = true)
    public long countByTestId(Long testId) {
        return assignmentRepository.countByTestId(testId);
    }

    @Transactional(readOnly = true)
    public Float getTotalPointsByTestId(Long testId) {
        return assignmentRepository.sumPointsByTestId(testId);
    }

    @Transactional(readOnly = true)
    public long countByType(AssignmentType type) {
        return assignmentRepository.countByType(type);
    }

    private Test getTestById(Long testId) {
        return testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found with id: " + testId));
    }

    private void validateAssignmentCreation(Test test, String title) {
        if (assignmentRepository.existsByTestIdAndTitle(test.getId(), title)) {
            throw new IllegalArgumentException("Assignment with title '" + title + "' already exists in this test");
        }
    }

    private void validateAssignment(Assignment assignment) {
        if (!assignment.isValid()) {
            throw new IllegalArgumentException("Assignment validation failed: invalid title or points");
        }

        ValidationResult typeValidation = assignment.validateAnswer("");
        if (assignment instanceof SingleChoiceAssignment || assignment instanceof MultipleChoiceAssignment) {
            validateChoiceOptions(assignment);
        }
    }

    private void validateChoiceOptions(Assignment assignment) {
        if (assignment instanceof SingleChoiceAssignment single) {
            List<ChoiceOption> options = single.getOptions();
            if (options == null || options.isEmpty()) {
                throw new IllegalArgumentException("Single choice assignment must have at least one option");
            }
            
            long correctCount = options.stream().mapToLong(opt -> opt.isCorrect() ? 1 : 0).sum();
            if (correctCount != 1) {
                throw new IllegalArgumentException("Single choice assignment must have exactly one correct option");
            }
        } else if (assignment instanceof MultipleChoiceAssignment multiple) {
            List<ChoiceOption> options = multiple.getOptions();
            if (options == null || options.size() < 2) {
                throw new IllegalArgumentException("Multiple choice assignment must have at least two options");
            }
            
            long correctCount = options.stream().mapToLong(opt -> opt.isCorrect() ? 1 : 0).sum();
            if (correctCount == 0) {
                throw new IllegalArgumentException("Multiple choice assignment must have at least one correct option");
            }
        }
    }

    private Integer getNextOrderNumber(Long testId) {
        Integer maxOrder = assignmentRepository.getMaxOrderNumberByTestId(testId);
        return maxOrder != null ? maxOrder + 1 : 1;
    }

    private Assignment createDuplicateAssignment(Assignment original, Integer newOrderNumber) {
        if (original instanceof SingleChoiceAssignment single) {
            return SingleChoiceAssignment.singleChoiceBuilder()
                    .testId(single.getTestId())
                    .title(single.getTitle() + " (Copy)")
                    .description(single.getDescription())
                    .points(single.getPoints())
                    .orderNumber(newOrderNumber)
                    .options(single.getOptions())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        } else if (original instanceof MultipleChoiceAssignment multiple) {
            return MultipleChoiceAssignment.multipleChoiceBuilder()
                    .testId(multiple.getTestId())
                    .title(multiple.getTitle() + " (Copy)")
                    .description(multiple.getDescription())
                    .points(multiple.getPoints())
                    .orderNumber(newOrderNumber)
                    .options(multiple.getOptions())
                    .randomizeOptions(multiple.isRandomizeOptions())
                    .partialScoring(multiple.isPartialScoring())
                    .penaltyForWrong(multiple.isPenaltyForWrong())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        } else if (original instanceof OpenQuestionAssignment open) {
            return OpenQuestionAssignment.builder()
                    .testId(open.getTestId())
                    .title(open.getTitle() + " (Copy)")
                    .description(open.getDescription())
                    .points(open.getPoints())
                    .orderNumber(newOrderNumber)
                    .isAttachmentAllowed(open.getIsAttachmentAllowed())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        } else if (original instanceof CodingAssignment coding) {
            return CodingAssignment.codingBuilder()
                    .testId(coding.getTestId())
                    .title(coding.getTitle() + " (Copy)")
                    .description(coding.getDescription())
                    .points(coding.getPoints())
                    .orderNumber(newOrderNumber)
                    .timeLimitMs(coding.getTimeLimitMs())
                    .memoryLimitMb(coding.getMemoryLimitMb())
                    .allowedLanguages(coding.getAllowedLanguages())
                    .starterCode(coding.getStarterCode())
                    .solutionTemplate(coding.getSolutionTemplate())
                    .testCases(coding.getTestCases())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        
        throw new IllegalArgumentException("Unknown assignment type: " + original.getClass().getSimpleName());
    }
}