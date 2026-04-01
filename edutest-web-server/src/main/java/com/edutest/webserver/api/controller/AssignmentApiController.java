package com.edutest.webserver.api.controller;

import com.edutest.api.AssignmentsApi;
import com.edutest.api.model.AssignmentResponse;
import com.edutest.api.model.ChoiceOptionRequest;
import com.edutest.api.model.CreateAssignmentRequest;
import com.edutest.api.model.MoveAssignmentRequest;
import com.edutest.api.model.TestCaseRequest;
import com.edutest.api.model.UpdateAssignmentRequest;
import com.edutest.domain.assignment.Assignment;
import com.edutest.domain.assignment.coding.TestCase;
import com.edutest.domain.assignment.common.ChoiceOption;
import com.edutest.service.assignmentservice.AssignmentService;
import com.edutest.util.AssignmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Slf4j
public class AssignmentApiController implements AssignmentsApi {

    private final AssignmentService assignmentService;
    private final AssignmentMapper assignmentMapper;

    @Override
    public ResponseEntity<List<AssignmentResponse>> getAssignments(Long testId) {
        log.info("Getting assignments for testId={}", testId);
        List<Assignment> assignments = assignmentService.findByTestId(testId);
        List<AssignmentResponse> result = assignments.stream()
                .map(assignmentMapper::toApiResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<AssignmentResponse> createAssignment(Long testId, CreateAssignmentRequest request) {
        log.info("Creating assignment type={} for testId={}", request.getType(), testId);

        Assignment created = switch (request.getType().name().toUpperCase()) {
            case "SINGLE_CHOICE" -> {
                List<ChoiceOption> options = mapChoiceOptions(request.getOptions());
                yield assignmentService.createSingleChoiceAssignment(
                        testId, request.getTitle(), request.getDescription(),
                        request.getPoints(), options);
            }
            case "MULTIPLE_CHOICE" -> {
                List<ChoiceOption> options = mapChoiceOptions(request.getOptions());
                yield assignmentService.createMultipleChoiceAssignment(
                        testId, request.getTitle(), request.getDescription(),
                        request.getPoints(), options,
                        Boolean.TRUE.equals(request.getRandomizeOptions()),
                        Boolean.TRUE.equals(request.getPartialScoring()),
                        Boolean.TRUE.equals(request.getPenaltyForWrong()));
            }
            case "OPEN_QUESTION" -> assignmentService.createOpenQuestionAssignment(
                    testId, request.getTitle(), request.getDescription(),
                    request.getPoints(), request.getMaxLength(), request.getMinLength(),
                    false);
            case "CODING" -> {
                List<TestCase> testCases = mapTestCases(request.getTestCases());
                yield assignmentService.createCodingAssignment(
                        testId, request.getTitle(), request.getDescription(),
                        request.getPoints(), request.getTimeLimitMs(), request.getMemoryLimitMb(),
                        request.getAllowedLanguages(), request.getStarterCode(),
                        request.getSolutionTemplate(), testCases);
            }
            default -> throw new IllegalArgumentException("Unknown assignment type: " + request.getType());
        };

        return ResponseEntity.status(201).body(assignmentMapper.toApiResponse(created));
    }

    @Override
    public ResponseEntity<AssignmentResponse> getAssignment(Long testId, Long assignmentId) {
        log.info("Getting assignment id={} for testId={}", assignmentId, testId);
        Assignment assignment = assignmentService.findById(assignmentId);
        return ResponseEntity.ok(assignmentMapper.toApiResponse(assignment));
    }

    @Override
    public ResponseEntity<AssignmentResponse> updateAssignment(Long testId, Long assignmentId, UpdateAssignmentRequest request) {
        log.info("Updating assignment id={}", assignmentId);
        Assignment updated = assignmentService.updateAssignment(
                assignmentId, request.getTitle(), request.getDescription(), request.getPoints());
        return ResponseEntity.ok(assignmentMapper.toApiResponse(updated));
    }

    @Override
    public ResponseEntity<Void> deleteAssignment(Long testId, Long assignmentId) {
        log.info("Deleting assignment id={}", assignmentId);
        assignmentService.deleteAssignment(assignmentId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AssignmentResponse> moveAssignment(Long testId, Long assignmentId, MoveAssignmentRequest request) {
        log.info("Moving assignment id={} to order={}", assignmentId, request.getNewOrderNumber());
        Assignment moved = assignmentService.moveAssignment(assignmentId, request.getNewOrderNumber());
        return ResponseEntity.ok(assignmentMapper.toApiResponse(moved));
    }

    @Override
    public ResponseEntity<AssignmentResponse> duplicateAssignment(Long testId, Long assignmentId) {
        log.info("Duplicating assignment id={}", assignmentId);
        Assignment duplicate = assignmentService.duplicateAssignment(assignmentId);
        return ResponseEntity.status(201).body(assignmentMapper.toApiResponse(duplicate));
    }

    private List<ChoiceOption> mapChoiceOptions(List<ChoiceOptionRequest> dtos) {
        if (dtos == null) return Collections.emptyList();
        List<ChoiceOption> options = new java.util.ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            ChoiceOptionRequest dto = dtos.get(i);
            ChoiceOption option = assignmentMapper.toChoiceOption(dto);
            // Set orderNumber based on index if not provided
            if (option.getOrderNumber() == null) {
                option = option.withOrderNumber(i + 1);
            }
            options.add(option);
        }
        return options;
    }

    private List<TestCase> mapTestCases(List<TestCaseRequest> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream()
                .map(assignmentMapper::toTestCase)
                .collect(Collectors.toList());
    }
}
