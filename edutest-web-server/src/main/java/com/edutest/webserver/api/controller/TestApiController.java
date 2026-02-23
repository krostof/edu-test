package com.edutest.webserver.api.controller;

import com.edutest.api.TestsApi;
import com.edutest.api.model.CreateTestRequest;
import com.edutest.api.model.Test;
import com.edutest.api.model.TestAttempt;
import com.edutest.api.model.TestDetails;
import com.edutest.domain.user.User;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.service.TestAttemptService;
import com.edutest.service.testservice.TestService;
import com.edutest.util.UserMapper;
import com.edutest.webserver.api.dto.UpdateTestRequest;
import com.edutest.webserver.api.helper.SecurityContextHelper;
import com.edutest.webserver.api.mapper.TestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Slf4j
public class TestApiController implements TestsApi {

    private final TestService testService;
    private final TestAttemptService testAttemptService;
    private final TestMapper testMapper;
    private final SecurityContextHelper securityContextHelper;
    private final UserMapper userMapper;

    @Override
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
        } else if (currentUser.getRole() == UserEntityRole.STUDENT) {
            tests = testService.findAvailableTestsForStudent(currentUser.getId());
        } else {
            tests = testService.findByCreatedBy(currentUser.getId());
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

        com.edutest.domain.test.Test test = testService.findById(testId);
        User student = userMapper.toUser(currentUser);

        com.edutest.domain.test.TestAttempt.AttemptResult result =
                testAttemptService.validateAndPrepareAttempt(test, student);

        if (!result.isSuccess()) {
            throw new IllegalStateException(result.getMessage());
        }

        return ResponseEntity.status(201).body(testMapper.toApiTestAttempt(result.getAttempt()));
    }

    @PutMapping("/tests/{testId}")
    public ResponseEntity<Test> updateTest(@PathVariable Long testId,
                                           @Valid @RequestBody UpdateTestRequest request) {
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

    @DeleteMapping("/tests/{testId}")
    public ResponseEntity<Void> deleteTest(@PathVariable Long testId) {
        log.info("Deleting test id={}", testId);
        testService.deleteTest(testId);
        return ResponseEntity.noContent().build();
    }
}
