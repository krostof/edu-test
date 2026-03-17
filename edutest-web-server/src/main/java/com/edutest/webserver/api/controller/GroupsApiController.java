package com.edutest.webserver.api.controller;

import com.edutest.api.GroupsApi;
import com.edutest.api.model.*;
import com.edutest.domain.group.StudentGroup;
import com.edutest.domain.user.User;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.service.groupservice.StudentGroupService;
import com.edutest.util.UserMapper;
import com.edutest.commons.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Slf4j
public class GroupsApiController implements GroupsApi {

    private final StudentGroupService studentGroupService;
    private final SecurityContextHelper securityContextHelper;
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<List<com.edutest.api.model.StudentGroup>> getGroups() {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Getting groups for user={}", currentUser.getId());

        List<StudentGroup> groups;
        if (currentUser.getRole() == UserEntityRole.STUDENT) {
            Optional<StudentGroup> studentGroup = studentGroupService.findByStudent(currentUser.getId());
            groups = studentGroup.map(List::of).orElse(List.of());
        } else {
            groups = studentGroupService.findByTeacher(currentUser.getId());
        }

        List<com.edutest.api.model.StudentGroup> result = groups.stream()
                .map(this::toApiStudentGroup)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.edutest.api.model.StudentGroup> createGroup(CreateGroupRequest request) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Creating group: name={}, teacherId={}", request.getName(), currentUser.getId());

        StudentGroup created = studentGroupService.createStudentGroup(
                request.getName(), request.getDescription(), currentUser.getId());

        log.info("Group created with id={}", created.getId());
        return ResponseEntity.status(201).body(toApiStudentGroup(created));
    }

    @Override
    public ResponseEntity<StudentGroupDetails> getGroupById(Long groupId) {
        log.info("Getting group by id={}", groupId);
        StudentGroup group = studentGroupService.findById(groupId);
        return ResponseEntity.ok(toApiStudentGroupDetails(group));
    }

    @Override
    public ResponseEntity<com.edutest.api.model.StudentGroup> updateGroup(Long groupId, UpdateGroupRequest request) {
        log.info("Updating group id={}", groupId);
        StudentGroup updated = studentGroupService.updateStudentGroup(
                groupId, request.getName(), request.getDescription());
        return ResponseEntity.ok(toApiStudentGroup(updated));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addStudentToGroup(Long groupId, AddStudentRequest request) {
        log.info("Adding student {} to group {}", request.getStudentId(), groupId);
        studentGroupService.addStudentToGroup(groupId, request.getStudentId());
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeStudentFromGroup(Long groupId, Long studentId) {
        log.info("Removing student {} from group {}", studentId, groupId);
        studentGroupService.removeStudentFromGroup(groupId, studentId);
        return ResponseEntity.noContent().build();
    }

    private com.edutest.api.model.StudentGroup toApiStudentGroup(StudentGroup domain) {
        com.edutest.api.model.StudentGroup api = new com.edutest.api.model.StudentGroup();
        api.setId(domain.getId());
        api.setName(domain.getName());
        api.setDescription(domain.getDescription());

        if (domain.getTeacher() != null) {
            api.setTeacher(toApiUserProfile(domain.getTeacher()));
        }

        return api;
    }

    private StudentGroupDetails toApiStudentGroupDetails(StudentGroup domain) {
        StudentGroupDetails details = new StudentGroupDetails();
        details.setId(domain.getId());
        details.setName(domain.getName());
        details.setDescription(domain.getDescription());

        if (domain.getTeacher() != null) {
            details.setTeacher(toApiUserProfile(domain.getTeacher()));
        }

        if (domain.getStudents() != null) {
            List<UserProfile> studentProfiles = domain.getStudents().stream()
                    .map(this::toApiUserProfile)
                    .collect(Collectors.toList());
            details.setStudents(studentProfiles);
        }

        return details;
    }

    private UserProfile toApiUserProfile(User user) {
        UserProfile profile = new UserProfile();
        profile.setUsername(user.getUsername());
        profile.setEmail(user.getEmail());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());

        userRepository.findByUsername(user.getUsername()).ifPresent(entity -> {
            profile.setId(entity.getId());
            profile.setIsActive(entity.getIsActive());
        });

        return profile;
    }
}
