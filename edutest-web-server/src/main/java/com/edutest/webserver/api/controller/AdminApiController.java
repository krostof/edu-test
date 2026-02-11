package com.edutest.webserver.api.controller;

import com.edutest.api.AdminApi;
import com.edutest.api.model.*;
import com.edutest.dto.BatchOperationResult;
import com.edutest.service.userservice.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Slf4j
public class AdminApiController implements AdminApi {

    private final UserManagementService userManagementService;

    @Override
    public ResponseEntity<UserProfile> createStudent(CreateStudentRequest createStudentRequest) {
        log.info("Creating student with email: {}", createStudentRequest.getEmail());
        UserProfile student = userManagementService.createStudent(createStudentRequest);
        log.info("Student created successfully: {}", student.getUsername());
        return ResponseEntity.status(201).body(student);
    }

    @Override
    public ResponseEntity<UserProfile> createTeacher(CreateTeacherRequest createTeacherRequest) {
        log.info("Creating teacher with username: {}", createTeacherRequest.getUsername());
        UserProfile teacher = userManagementService.createTeacher(createTeacherRequest);
        log.info("Teacher created successfully: {}", teacher.getUsername());
        return ResponseEntity.status(201).body(teacher);
    }

    @Override
    public ResponseEntity<UserPageResponse> getAllUsers(UserRole role, Integer page, Integer size, String search) {
        log.info("Getting all users - role: {}, page: {}, size: {}, search: {}", role, page, size, search);
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        UserPageResponse response = userManagementService.getAllUsers(role, pageNum, pageSize, search);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UserProfile> getUserById(Long userId) {
        log.info("Getting user by id: {}", userId);
        UserProfile user = userManagementService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @Override
    public ResponseEntity<UserProfile> updateUser(Long userId, UpdateUserRequest updateUserRequest) {
        log.info("Updating user: {}", userId);
        UserProfile updatedUser = userManagementService.updateUser(userId, updateUserRequest);
        log.info("User {} updated successfully", userId);
        return ResponseEntity.ok(updatedUser);
    }

    @Override
    public ResponseEntity<Void> deleteUser(Long userId) {
        log.info("Deleting user: {}", userId);
        userManagementService.deleteUserWithValidation(userId);
        log.info("User {} deleted successfully", userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserProfile> activateUser(Long userId) {
        log.info("Activating user: {}", userId);
        UserProfile activatedUser = userManagementService.activateUser(userId);
        return ResponseEntity.ok(activatedUser);
    }

    @Override
    public ResponseEntity<UserProfile> deactivateUser(Long userId) {
        log.info("Deactivating user: {}", userId);
        UserProfile deactivatedUser = userManagementService.deactivateUser(userId);
        return ResponseEntity.ok(deactivatedUser);
    }

    @Override
    public ResponseEntity<BatchOperationResponse> batchDeactivateUsers(BatchUserIdsRequest batchUserIdsRequest) {
        log.info("Batch deactivating {} users", batchUserIdsRequest.getUserIds().size());
        BatchOperationResult result = userManagementService.batchDeactivateUsers(batchUserIdsRequest.getUserIds());

        BatchOperationResponse response = new BatchOperationResponse();
        response.setSuccessCount(result.getSuccessCount());
        response.setFailedCount(result.getFailedCount());
        response.setErrors(result.getErrors());

        log.info("Batch deactivation completed: {} succeeded, {} failed",
                 result.getSuccessCount(), result.getFailedCount());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<BatchOperationResponse> batchDeleteUsers(BatchUserIdsRequest batchUserIdsRequest) {
        log.info("Batch deleting {} users", batchUserIdsRequest.getUserIds().size());
        BatchOperationResult result = userManagementService.batchDeleteUsers(batchUserIdsRequest.getUserIds());

        BatchOperationResponse response = new BatchOperationResponse();
        response.setSuccessCount(result.getSuccessCount());
        response.setFailedCount(result.getFailedCount());
        response.setErrors(result.getErrors());

        log.info("Batch deletion completed: {} succeeded, {} failed",
                 result.getSuccessCount(), result.getFailedCount());
        return ResponseEntity.ok(response);
    }
}
