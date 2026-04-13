package com.edutest.webserver.api.controller;

import com.edutest.api.UsersApi;
import com.edutest.api.model.UpdateProfileRequest;
import com.edutest.api.model.UserProfile;
import com.edutest.commons.SecurityContextHelper;
import com.edutest.domain.user.User;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.service.userservice.UserService;
import com.edutest.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Slf4j
public class UserApiController implements UsersApi {

    private final UserService userService;
    private final SecurityContextHelper securityContextHelper;
    private final UserMapper userMapper;

    @Override
    public ResponseEntity<UserProfile> getCurrentUserProfile() {
        log.info("Getting current user profile");
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        UserProfile profile = userMapper.toUserProfile(currentUser);
        return ResponseEntity.ok(profile);
    }

    @Override
    public ResponseEntity<UserProfile> updateCurrentUserProfile(UpdateProfileRequest request) {
        log.info("Updating current user profile");
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();

        User updatedUser = userService.updateUserProfile(
                currentUser.getId(),
                request.getFirstName(),
                request.getLastName(),
                request.getEmail()
        );

        // Fetch the updated entity to build the profile response
        UserEntity updatedEntity = securityContextHelper.getCurrentUserEntity();
        UserProfile profile = userMapper.toUserProfile(updatedEntity);

        return ResponseEntity.ok(profile);
    }
}
