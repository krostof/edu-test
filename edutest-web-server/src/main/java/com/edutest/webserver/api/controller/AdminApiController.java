package com.edutest.webserver.api.controller;

import com.edutest.api.AdminApi;
import com.edutest.api.model.CreateStudentRequest;
import com.edutest.api.model.UserProfile;
import com.edutest.service.userservice.UserManagementService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class AdminApiController implements AdminApi {

    private final UserManagementService userManagementService;

    @Override
    public ResponseEntity<UserProfile> createStudent(@Valid CreateStudentRequest body) {

        UserProfile student = userManagementService.createStudent(body);
        return ResponseEntity.status(201).body(student);

    }

}
