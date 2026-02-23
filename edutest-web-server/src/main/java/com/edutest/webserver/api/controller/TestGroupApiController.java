package com.edutest.webserver.api.controller;

import com.edutest.domain.group.StudentGroup;
import com.edutest.service.testservice.TestService;
import com.edutest.webserver.api.dto.AssignGroupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/tests/{testId}/groups")
@Slf4j
public class TestGroupApiController {

    private final TestService testService;

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getTestGroups(@PathVariable Long testId) {
        log.info("Getting groups for testId={}", testId);
        List<StudentGroup> groups = testService.getTestGroups(testId);
        List<GroupResponse> result = groups.stream()
                .map(this::toGroupResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Void> assignGroup(
            @PathVariable Long testId,
            @Valid @RequestBody AssignGroupRequest request) {
        log.info("Assigning group {} to test {}", request.getGroupId(), testId);
        testService.assignGroupToTest(testId, request.getGroupId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> removeGroup(
            @PathVariable Long testId,
            @PathVariable Long groupId) {
        log.info("Removing group {} from test {}", groupId, testId);
        testService.removeGroupFromTest(testId, groupId);
        return ResponseEntity.noContent().build();
    }

    private GroupResponse toGroupResponse(StudentGroup group) {
        GroupResponse resp = new GroupResponse();
        resp.setId(group.getId());
        resp.setName(group.getName());
        resp.setDescription(group.getDescription());
        resp.setStudentCount(group.getStudentCount());
        return resp;
    }

    @lombok.Data
    public static class GroupResponse {
        private Long id;
        private String name;
        private String description;
        private Integer studentCount;
    }
}
