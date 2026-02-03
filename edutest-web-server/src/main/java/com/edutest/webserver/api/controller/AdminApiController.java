package com.edutest.webserver.api.controller;

import com.edutest.api.AdminApi;
import com.edutest.api.model.CreateStudentRequest;
import com.edutest.api.model.CreateTeacherRequest;
import com.edutest.api.model.UserProfile;
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
        log.info("Creating student with username: {}", createStudentRequest.getUsername());

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

}
