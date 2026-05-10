package com.edutest.webserver.api.controller;

import com.edutest.api.TestsApi;
import com.edutest.api.model.*;
import com.edutest.domain.user.User;
import com.edutest.dto.AnswerDto;
import com.edutest.dto.AnswerReviewDto;
import com.edutest.dto.AttemptListItemDto;
import com.edutest.dto.GradeAnswerRequestDto;
import com.edutest.dto.SubmitAnswerRequestDto;
import com.edutest.dto.TestResultResponseDto;
import com.edutest.dto.TestStatsSummaryDto;
import com.edutest.dto.TestSubmissionResultDto;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.dto.AttemptIncidentDto;
import com.edutest.dto.RecordIncidentRequestDto;
import com.edutest.dto.RunStatusDto;
import com.edutest.dto.SubmitStatusDto;
import com.edutest.service.TestAttemptService;
import com.edutest.service.attempt.TestAttemptManagementService;
import com.edutest.service.answer.AnswerSubmissionService;
import com.edutest.service.answer.AsyncTestSubmissionService;
import com.edutest.service.answer.SubmitJobRegistry;
import com.edutest.service.incident.AttemptIncidentService;
import com.edutest.service.answer.TestResultsService;
import com.edutest.service.answer.TestSubmissionService;
import com.edutest.service.teacher.ManualGradingService;
import com.edutest.service.teacher.TeacherAttemptService;
import com.edutest.service.teacher.TestResultsExportService;
import com.edutest.service.testservice.TestService;
import com.edutest.service.groupservice.StudentGroupService;
import com.edutest.service.attempt.TestAttemptStateService;
import com.edutest.dto.TestAttemptStateDto;
import com.edutest.dto.QuestionViewDto;
import com.edutest.util.AnswerMapper;
import com.edutest.util.TeacherMapper;
import com.edutest.util.UserMapper;
import com.edutest.commons.SecurityContextHelper;
import com.edutest.util.TestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Slf4j
public class TestApiController implements TestsApi {

    private final TestService testService;
    private final StudentGroupService studentGroupService;
    private final TestAttemptService testAttemptService;
    private final TestAttemptManagementService testAttemptManagementService;
    private final TestMapper testMapper;
    private final SecurityContextHelper securityContextHelper;
    private final UserMapper userMapper;
    private final AnswerSubmissionService answerSubmissionService;
    private final TestSubmissionService testSubmissionService;
    private final AsyncTestSubmissionService asyncTestSubmissionService;
    private final SubmitJobRegistry submitJobRegistry;
    private final TestResultsService testResultsService;
    private final AnswerMapper answerMapper;
    private final TeacherAttemptService teacherAttemptService;
    private final ManualGradingService gradingService;
    private final TestResultsExportService exportService;
    private final TeacherMapper teacherMapper;
    private final TestAttemptStateService attemptStateService;
    private final AttemptIncidentService incidentService;

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Test> createTest(CreateTestRequest request) {
        log.info("Creating test: {}", request.getTitle());
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();

        LocalDateTime startDate = request.getStartDate() != null
                ? request.getStartDate().toLocalDateTime() : null;
        LocalDateTime endDate = request.getEndDate() != null
                ? request.getEndDate().toLocalDateTime() : null;

        com.edutest.domain.test.Test created = testService.createTest(
                request.getTitle(),
                request.getDescription(),
                startDate,
                endDate,
                request.getTimeLimit(),
                request.getAllowNavigation(),
                request.getRandomizeOrder(),
                currentUser.getId()
        );

        log.info("Test created with id={}", created.getId());
        return ResponseEntity.status(201).body(testMapper.toApiTest(created));
    }

    @Override
    public ResponseEntity<List<Test>> getTests(Long groupId) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Getting tests for user={}, groupId={}", currentUser.getId(), groupId);

        List<com.edutest.domain.test.Test> tests;

        if (groupId != null) {
            tests = testService.findTestsByGroup(groupId);
        } else if (currentUser.isStudent()) {
            tests = testService.findAvailableTestsForStudent(currentUser.getId());
        } else if (currentUser.isTeacher()) {
            tests = testService.findByCreatedBy(currentUser.getId());
        } else {
            tests = List.of();
        }

        List<Test> result = tests.stream()
                .map(testMapper::toApiTest)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<TestDetails> getTestById(Long testId) {
        log.info("Getting test by id={}", testId);
        com.edutest.domain.test.Test test = testService.findById(testId);
        return ResponseEntity.ok(testMapper.toApiTestDetails(test));
    }

    @Override
    public ResponseEntity<TestAttempt> startTestAttempt(Long testId) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Starting test attempt: testId={}, studentId={}", testId, currentUser.getId());

        TestAttemptEntity attemptEntity = testAttemptManagementService.startOrResumeAttempt(
                testId, currentUser.getId());

        return ResponseEntity.status(201).body(testMapper.toApiTestAttempt(attemptEntity));
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Test> updateTest(Long testId, UpdateTestRequest request) {
        log.info("Updating test id={}", testId);

        LocalDateTime startDate = request.getStartDate() != null
                ? request.getStartDate().toLocalDateTime() : null;
        LocalDateTime endDate = request.getEndDate() != null
                ? request.getEndDate().toLocalDateTime() : null;

        com.edutest.domain.test.Test updated = testService.updateTest(
                testId,
                request.getTitle(),
                request.getDescription(),
                startDate,
                endDate,
                request.getTimeLimit(),
                request.getAllowNavigation(),
                request.getRandomizeOrder()
        );

        return ResponseEntity.ok(testMapper.toApiTest(updated));
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteTest(Long testId) {
        log.info("Deleting test id={}", testId);
        testService.deleteTest(testId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<TestGroupResponse>> getTestGroups(Long testId) {
        log.info("Getting groups for testId={}", testId);
        List<com.edutest.domain.group.StudentGroup> groups = testService.getTestGroups(testId);
        List<TestGroupResponse> result = groups.stream()
                .map(this::toTestGroupResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<StudentGroup>> getAvailableGroupsForTest(Long testId) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Getting available groups for testId={}, teacherId={}", testId, currentUser.getId());

        // Get teacher's groups
        List<com.edutest.domain.group.StudentGroup> teacherGroups =
                studentGroupService.findByTeacher(currentUser.getId());

        // Get groups already assigned to test
        List<com.edutest.domain.group.StudentGroup> assignedGroups = testService.getTestGroups(testId);
        List<Long> assignedGroupIds = assignedGroups.stream()
                .map(com.edutest.domain.group.StudentGroup::getId)
                .toList();

        // Filter out already assigned groups
        List<StudentGroup> availableGroups = teacherGroups.stream()
                .filter(g -> !assignedGroupIds.contains(g.getId()))
                .map(this::toApiStudentGroup)
                .collect(Collectors.toList());

        return ResponseEntity.ok(availableGroups);
    }

    private StudentGroup toApiStudentGroup(com.edutest.domain.group.StudentGroup domain) {
        StudentGroup api = new StudentGroup();
        api.setId(domain.getId());
        api.setName(domain.getName());
        api.setDescription(domain.getDescription());
        return api;
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> assignGroupToTest(Long testId, AssignGroupRequest request) {
        log.info("Assigning group {} to test {}", request.getGroupId(), testId);
        testService.assignGroupToTest(testId, request.getGroupId());
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> removeGroupFromTest(Long testId, Long groupId) {
        log.info("Removing group {} from test {}", groupId, testId);
        testService.removeGroupFromTest(testId, groupId);
        return ResponseEntity.noContent().build();
    }

    private TestGroupResponse toTestGroupResponse(com.edutest.domain.group.StudentGroup group) {
        TestGroupResponse resp = new TestGroupResponse();
        resp.setId(group.getId());
        resp.setName(group.getName());
        resp.setDescription(group.getDescription());
        resp.setStudentCount(group.getStudentCount());
        return resp;
    }

    @Override
    public ResponseEntity<AnswerResponse> submitAnswer(Long testId, Long attemptId, Long assignmentId, SubmitAnswerRequest request) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Submitting answer: testId={}, attemptId={}, assignmentId={}, studentId={}",
                testId, attemptId, assignmentId, currentUser.getId());

        SubmitAnswerRequestDto requestDto = answerMapper.fromApiSubmitAnswerRequest(request);
        AnswerDto answerDto = answerSubmissionService.submitAnswer(
                testId, attemptId, assignmentId, currentUser.getId(), requestDto);

        return ResponseEntity.ok(answerMapper.toApiAnswerResponse(answerDto));
    }

    @PostMapping("/tests/{testId}/attempts/{attemptId}/incidents")
    public ResponseEntity<Void> recordIncident(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            @RequestBody RecordIncidentRequestDto request) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        // fire-and-forget from frontend's perspective; we still validate ownership
        incidentService.recordIncident(testId, attemptId, currentUser.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tests/{testId}/attempts/{attemptId}/incidents")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<AttemptIncidentDto>> listIncidents(
            @PathVariable Long testId,
            @PathVariable Long attemptId) {
        return ResponseEntity.ok(incidentService.listIncidents(testId, attemptId));
    }

    @PostMapping("/tests/{testId}/attempts/{attemptId}/answers/{assignmentId}/run")
    public ResponseEntity<RunStatusDto> runCode(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            @PathVariable Long assignmentId) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Triggering async run: testId={}, attemptId={}, assignmentId={}, studentId={}",
                testId, attemptId, assignmentId, currentUser.getId());

        RunStatusDto status = answerSubmissionService.runCode(
                testId, attemptId, assignmentId, currentUser.getId());

        // 202 Accepted — work is queued, client should poll for completion
        return ResponseEntity.accepted().body(status);
    }

    @GetMapping("/tests/{testId}/attempts/{attemptId}/answers/{assignmentId}/run-status")
    public ResponseEntity<RunStatusDto> getRunStatus(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            @PathVariable Long assignmentId) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        RunStatusDto status = answerSubmissionService.getRunStatus(
                testId, attemptId, assignmentId, currentUser.getId());
        return ResponseEntity.ok(status);
    }

    @Override
    public ResponseEntity<AnswerResponse> getAnswer(Long testId, Long attemptId, Long assignmentId) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Getting answer: testId={}, attemptId={}, assignmentId={}, studentId={}",
                testId, attemptId, assignmentId, currentUser.getId());

        Optional<AnswerDto> answerDto = answerSubmissionService.getAnswer(
                testId, attemptId, assignmentId, currentUser.getId());

        return answerDto
                .map(dto -> ResponseEntity.ok(answerMapper.toApiAnswerResponse(dto)))
                .orElseThrow(() -> new IllegalArgumentException("Answer not found"));
    }

    @Override
    public ResponseEntity<List<AnswerResponse>> getAllAnswers(Long testId, Long attemptId) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Getting all answers: testId={}, attemptId={}, studentId={}",
                testId, attemptId, currentUser.getId());

        List<AnswerDto> answers = answerSubmissionService.getAllAnswers(
                testId, attemptId, currentUser.getId());

        List<AnswerResponse> result = answers.stream()
                .map(answerMapper::toApiAnswerResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<SubmitStatus> submitTestAttempt(Long testId, Long attemptId) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Triggering async submit: testId={}, attemptId={}, studentId={}",
                testId, attemptId, currentUser.getId());

        // Pre-flight ownership/state check on the request thread, so 403/404/409
        // come back as proper HTTP errors instead of being buried in the async job.
        testSubmissionService.assertSubmittable(testId, attemptId, currentUser.getId());

        submitJobRegistry.markPending(attemptId);
        asyncTestSubmissionService.executeAsync(testId, attemptId, currentUser.getId());

        SubmitStatusDto status = submitJobRegistry.getStatus(attemptId);
        return ResponseEntity.accepted().body(answerMapper.toApiSubmitStatus(status));
    }

    @Override
    public ResponseEntity<SubmitStatus> getSubmitStatus(Long testId, Long attemptId) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        testSubmissionService.assertCanReadAttempt(testId, attemptId, currentUser.getId());
        SubmitStatusDto status = submitJobRegistry.getStatus(attemptId);
        return ResponseEntity.ok(answerMapper.toApiSubmitStatus(status));
    }

    @Override
    public ResponseEntity<TestResultResponse> getTestResults(Long testId, Long attemptId) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Getting test results: testId={}, attemptId={}, studentId={}",
                testId, attemptId, currentUser.getId());

        TestResultResponseDto resultDto = testResultsService.getTestResults(
                testId, attemptId, currentUser.getId());

        return ResponseEntity.ok(answerMapper.toApiTestResultResponse(resultDto));
    }

    // ===================== Test Attempt State Endpoints =====================

    @Override
    public ResponseEntity<TestAttemptState> getTestAttemptState(Long testId, Long attemptId) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Getting test attempt state: testId={}, attemptId={}, studentId={}",
                testId, attemptId, currentUser.getId());

        TestAttemptStateDto stateDto = attemptStateService.getAttemptState(
                testId, attemptId, currentUser.getId());

        return ResponseEntity.ok(testMapper.toApiAttemptState(stateDto));
    }

    @Override
    public ResponseEntity<QuestionView> getQuestionByIndex(Long testId, Long attemptId, Integer questionIndex) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Getting question by index: testId={}, attemptId={}, index={}, studentId={}",
                testId, attemptId, questionIndex, currentUser.getId());

        QuestionViewDto questionDto = attemptStateService.getQuestionByIndex(
                testId, attemptId, questionIndex, currentUser.getId());

        return ResponseEntity.ok(testMapper.toApiQuestionView(questionDto));
    }

    @Override
    public ResponseEntity<Void> updateNavigation(Long testId, Long attemptId, UpdateNavigationRequest request) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Updating navigation: testId={}, attemptId={}, index={}, studentId={}",
                testId, attemptId, request.getQuestionIndex(), currentUser.getId());

        attemptStateService.updateNavigation(testId, attemptId, request.getQuestionIndex(), currentUser.getId());

        return ResponseEntity.ok().build();
    }

    // ===================== Phase 2: Teacher Panel Endpoints =====================

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AttemptPageResponse> getTestAttempts(
            Long testId,
            Long groupId,
            String status,
            Integer page,
            Integer size,
            String sortBy,
            String sortDir) {
        log.info("Getting test attempts: testId={}, groupId={}, status={}, page={}, size={}",
                testId, groupId, status, page, size);

        Page<AttemptListItemDto> attemptsPage = teacherAttemptService.getAttemptsByTestId(
                testId,
                groupId,
                status,
                page != null ? page : 0,
                size != null ? size : 20,
                sortBy != null ? sortBy : "startedAt",
                sortDir != null ? sortDir : "desc");

        return ResponseEntity.ok(teacherMapper.toApiAttemptPageResponse(attemptsPage));
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TestStatsSummary> getTestAttemptsSummary(Long testId) {
        log.info("Getting test statistics summary: testId={}", testId);

        TestStatsSummaryDto summaryDto = teacherAttemptService.getTestStatsSummary(testId);

        return ResponseEntity.ok(teacherMapper.toApiTestStatsSummary(summaryDto));
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AnswerReviewResponse> getAnswerForReview(Long testId, Long attemptId, Long assignmentId) {
        log.info("Getting answer for review: testId={}, attemptId={}, assignmentId={}",
                testId, attemptId, assignmentId);

        AnswerReviewDto reviewDto = gradingService.getAnswerForReview(testId, attemptId, assignmentId);

        return ResponseEntity.ok(teacherMapper.toApiAnswerReviewResponse(reviewDto));
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AnswerReviewResponse> gradeAnswer(
            Long testId,
            Long attemptId,
            Long assignmentId,
            GradeAnswerRequest request) {
        log.info("Grading answer: testId={}, attemptId={}, assignmentId={}, score={}",
                testId, attemptId, assignmentId, request.getScore());

        GradeAnswerRequestDto requestDto = teacherMapper.fromApiGradeAnswerRequest(request);
        AnswerReviewDto reviewDto = gradingService.gradeAnswer(testId, attemptId, assignmentId, requestDto);

        return ResponseEntity.ok(teacherMapper.toApiAnswerReviewResponse(reviewDto));
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Resource> exportTestResults(Long testId, String format) {
        log.info("Exporting test results: testId={}, format={}", testId, format);

        String csvContent = exportService.exportToCsv(testId);

        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(bytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test_" + testId + "_results.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(bytes.length)
                .body(resource);
    }
}
