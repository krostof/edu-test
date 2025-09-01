package com.edutest.webserver.api.controller;

import com.edutest.api.model.UserProfile;
import com.edutest.api.model.UserRole;
import com.edutest.api.model.UserSecurity;
import com.edutest.commons.security.JwtTokenProvider;
import com.edutest.commons.security.LoginAndRegisterFacade;
import com.edutest.commons.security.UserProfileMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController{

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final LoginAndRegisterFacade loginAndRegisterFacade;
    private final UserProfileMapper userProfileMapper;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            String jwt = tokenProvider.generateJwtToken(authentication);
            
            return ResponseEntity.ok(new JwtResponse(jwt, "Bearer"));
        } catch (AuthenticationException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Invalid username or password"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            UserSecurity user = loginAndRegisterFacade.registerUser(
                    LoginAndRegisterFacade.RegisterRequest.builder()
                            .username(registerRequest.getUsername())
                            .email(registerRequest.getEmail())
                            .password(registerRequest.getPassword())
                            .firstName(registerRequest.getFirstName())
                            .lastName(registerRequest.getLastName())
                            .role(toUserEntityRole(registerRequest.getRole()))
                            .studentNumber(registerRequest.getStudentNumber())
                            .build()
            );

            UserProfile userProfile = userProfileMapper.toUserProfile(user);
            return ResponseEntity.ok(new RegisterResponse("User registered successfully", userProfile));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(e.getMessage()));
        }
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank
        @Size(min = 6, max = 100)
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank
        @Email
        @Size(max = 100)
        private String email;

        @NotBlank
        @Size(min = 6, max = 100)
        private String password;

        @NotBlank
        @Size(max = 50)
        private String firstName;

        @NotBlank
        @Size(max = 50)
        private String lastName;

        private UserRole role;

        @Size(max = 20)
        private String studentNumber;
    }

    private com.edutest.persistance.entity.user.UserEntityRole toUserEntityRole(UserRole userRole) {
        if (userRole == null) {
            return com.edutest.persistance.entity.user.UserEntityRole.STUDENT; // default
        }
        return switch (userRole) {
            case STUDENT -> com.edutest.persistance.entity.user.UserEntityRole.STUDENT;
            case TEACHER -> com.edutest.persistance.entity.user.UserEntityRole.TEACHER;
            case ADMIN -> com.edutest.persistance.entity.user.UserEntityRole.ADMIN;
        };
    }

    @Data
    public static class JwtResponse {
        private String token;
        private String type;

        public JwtResponse(String accessToken, String tokenType) {
            this.token = accessToken;
            this.type = tokenType;
        }
    }

    @Data
    public static class MessageResponse {
        private String message;

        public MessageResponse(String message) {
            this.message = message;
        }
    }

    @Data
    public static class RegisterResponse {
        private String message;
        private UserProfile user;

        public RegisterResponse(String message, UserProfile user) {
            this.message = message;
            this.user = user;
        }
    }
}