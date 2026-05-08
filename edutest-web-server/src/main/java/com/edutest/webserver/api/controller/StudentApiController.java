package com.edutest.webserver.api.controller;

import com.edutest.api.StudentApi;
import com.edutest.api.model.StudentTestResponse;
import com.edutest.dto.StudentTestDto;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.service.testservice.TestService;
import com.edutest.commons.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Slf4j
public class StudentApiController implements StudentApi {

    private final TestService testService;
    private final SecurityContextHelper securityContextHelper;

    @Override
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentTestResponse>> getStudentTests(String timeStatus) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Getting tests for student={}, timeStatus={}", currentUser.getId(), timeStatus);

        List<StudentTestDto> tests = testService.findAllTestsForStudentWithStatus(
                currentUser.getId(), timeStatus);

        List<StudentTestResponse> response = tests.stream()
                .map(this::toStudentTestResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentTestResponse> getStudentTestById(Long testId) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Getting test details for student={}, testId={}", currentUser.getId(), testId);

        List<StudentTestDto> tests = testService.findAllTestsForStudentWithStatus(
                currentUser.getId(), "ALL");

        return tests.stream()
                .filter(t -> t.getId().equals(testId))
                .findFirst()
                .map(dto -> ResponseEntity.ok(toStudentTestResponse(dto)))
                .orElseThrow(() -> new IllegalArgumentException("Test not found or not accessible"));
    }

    private StudentTestResponse toStudentTestResponse(StudentTestDto dto) {
        StudentTestResponse response = new StudentTestResponse();
        response.setId(dto.getId());
        response.setTitle(dto.getTitle());
        response.setDescription(dto.getDescription());
        response.setStartDate(toOffsetDateTime(dto.getStartDate()));
        response.setEndDate(toOffsetDateTime(dto.getEndDate()));
        response.setTimeLimit(dto.getTimeLimit());
        response.setAllowNavigation(dto.getAllowNavigation());
        response.setAssignmentCount(dto.getAssignmentCount());
        response.setTimeStatus(mapTimeStatus(dto.getTimeStatus()));
        response.setAttemptStatus(mapAttemptStatus(dto.getAttemptStatus()));
        response.setAttemptId(dto.getAttemptId());
        response.setAttemptStartedAt(toOffsetDateTime(dto.getAttemptStartedAt()));
        response.setAttemptFinishedAt(toOffsetDateTime(dto.getAttemptFinishedAt()));
        response.setScore(dto.getScore());
        response.setMaxScore(dto.getMaxScore());
        response.setCreatedByName(dto.getCreatedByName());
        return response;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atOffset(ZoneOffset.UTC);
    }

    private StudentTestResponse.TimeStatusEnum mapTimeStatus(String status) {
        if (status == null) return null;
        return StudentTestResponse.TimeStatusEnum.fromValue(status);
    }

    private StudentTestResponse.AttemptStatusEnum mapAttemptStatus(String status) {
        if (status == null) return null;
        return StudentTestResponse.AttemptStatusEnum.fromValue(status);
    }
}
