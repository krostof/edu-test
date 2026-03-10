package com.edutest.webserver.api.controller;

import com.edutest.domain.assignment.Assignment;
import com.edutest.domain.assignment.coding.TestCase;
import com.edutest.domain.assignment.common.ChoiceOption;
import com.edutest.dto.*;
import com.edutest.service.assignmentservice.AssignmentService;
import com.edutest.util.AssignmentMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/tests/{testId}/assignments")
@Slf4j
public class AssignmentApiController {

    private final AssignmentService assignmentService;
    private final AssignmentMapper assignmentMapper;

    @GetMapping
    public ResponseEntity<List<AssignmentResponse>> getAssignments(@PathVariable Long testId) {
        log.info("Getting assignments for testId={}", testId);
        List<Assignment> assignments = assignmentService.findByTestId(testId);
        List<AssignmentResponse> result = assignments.stream()
                .map(assignmentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<AssignmentResponse> createAssignment(
            @PathVariable Long testId,
            @Valid @RequestBody CreateAssignmentRequest request) {
        log.info("Creating assignment type={} for testId={}", request.getType(), testId);

        Assignment created = switch (request.getType().toUpperCase()) {
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

        return ResponseEntity.status(201).body(assignmentMapper.toResponse(created));
    }

    @GetMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponse> getAssignment(
            @PathVariable Long testId,
            @PathVariable Long assignmentId) {
        log.info("Getting assignment id={} for testId={}", assignmentId, testId);
        Assignment assignment = assignmentService.findById(assignmentId);
        return ResponseEntity.ok(assignmentMapper.toResponse(assignment));
    }

    @PutMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable Long testId,
            @PathVariable Long assignmentId,
            @Valid @RequestBody UpdateAssignmentRequest request) {
        log.info("Updating assignment id={}", assignmentId);
        Assignment updated = assignmentService.updateAssignment(
                assignmentId, request.getTitle(), request.getDescription(), request.getPoints());
        return ResponseEntity.ok(assignmentMapper.toResponse(updated));
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable Long testId,
            @PathVariable Long assignmentId) {
        log.info("Deleting assignment id={}", assignmentId);
        assignmentService.deleteAssignment(assignmentId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{assignmentId}/move")
    public ResponseEntity<AssignmentResponse> moveAssignment(
            @PathVariable Long testId,
            @PathVariable Long assignmentId,
            @RequestBody MoveAssignmentRequest request) {
        log.info("Moving assignment id={} to order={}", assignmentId, request.getNewOrderNumber());
        Assignment moved = assignmentService.moveAssignment(assignmentId, request.getNewOrderNumber());
        return ResponseEntity.ok(assignmentMapper.toResponse(moved));
    }

    @PostMapping("/{assignmentId}/duplicate")
    public ResponseEntity<AssignmentResponse> duplicateAssignment(
            @PathVariable Long testId,
            @PathVariable Long assignmentId) {
        log.info("Duplicating assignment id={}", assignmentId);
        Assignment duplicate = assignmentService.duplicateAssignment(assignmentId);
        return ResponseEntity.status(201).body(assignmentMapper.toResponse(duplicate));
    }

    private List<ChoiceOption> mapChoiceOptions(List<ChoiceOptionDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream()
                .map(assignmentMapper::toChoiceOption)
                .collect(Collectors.toList());
    }

    private List<TestCase> mapTestCases(List<TestCaseDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream()
                .map(assignmentMapper::toTestCase)
                .collect(Collectors.toList());
    }
}
