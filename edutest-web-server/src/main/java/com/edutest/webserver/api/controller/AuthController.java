package com.edutest.webserver.api.controller;

import com.edutest.api.AuthenticationApi;
import com.edutest.api.model.LoginRequest;
import com.edutest.api.model.LoginResponse;
import com.edutest.api.model.RefreshTokenRequest;
import com.edutest.api.model.UserProfile;
import com.edutest.api.model.UserRole;
import com.edutest.api.model.UserSecurity;
import com.edutest.commons.security.JwtTokenProvider;
import com.edutest.persistance.entity.auth.RefreshTokenEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.service.auth.RefreshTokenService;
import com.edutest.service.security.LoginAndRegisterFacade;
import com.edutest.service.security.UserProfileMapper;
import com.edutest.util.UserMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api")
public class AuthController implements AuthenticationApi {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final LoginAndRegisterFacade loginAndRegisterFacade;
    private final UserRepository userRepository;
    private final UserProfileMapper userProfileMapper;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @Value("${app.jwtExpirationMs:86400000}")
    private long jwtExpirationMs;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            LoginAndRegisterFacade loginAndRegisterFacade,
            UserRepository userRepository,
            @Qualifier("securityUserProfileMapper") UserProfileMapper userProfileMapper,
            RefreshTokenService refreshTokenService,
            UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.loginAndRegisterFacade = loginAndRegisterFacade;
        this.userRepository = userRepository;
        this.userProfileMapper = userProfileMapper;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
    }

    @Override
    public ResponseEntity<LoginResponse> login(@Valid LoginRequest loginRequest) {
        log.info("Login attempt for username: {}", loginRequest.getUsername());

        Optional<UserEntity> byUsername = userRepository.findByUsername(loginRequest.getUsername());

        if (byUsername.isEmpty()) {
            log.info("Username not found for username: {}", loginRequest.getUsername());
            return ResponseEntity.status(401).build();
        }

        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
            );

            Authentication authentication = authenticationManager.authenticate(authToken);
            log.info("Authentication successful for user: {}", loginRequest.getUsername());

            String jwt = tokenProvider.generateJwtToken(authentication);

            UserEntity user = byUsername.get();
            RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(user);

            UserProfile userProfile = userMapper.toUserProfile(user);

            LoginResponse response = new LoginResponse()
                    .accessToken(jwt)
                    .refreshToken(refreshToken.getToken())
                    .tokenType("Bearer")
                    .expiresIn(jwtExpirationMs / 1000)
                    .user(userProfile);

            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for username: {} - {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @Override
    public ResponseEntity<LoginResponse> refreshToken(@Valid RefreshTokenRequest refreshTokenRequest) {
        log.info("Refresh token request received");

        String requestRefreshToken = refreshTokenRequest.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshTokenEntity::getUser)
                .map(user -> {
                    String jwt = tokenProvider.generateJwtToken(user.getUsername());

                    RefreshTokenEntity newRefreshToken = refreshTokenService.createRefreshToken(user);
                    UserProfile userProfile = userMapper.toUserProfile(user);

                    LoginResponse response = new LoginResponse()
                            .accessToken(jwt)
                            .refreshToken(newRefreshToken.getToken())
                            .tokenType("Bearer")
                            .expiresIn(jwtExpirationMs / 1000)
                            .user(userProfile);

                    log.info("Token refreshed for user: {}", user.getUsername());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("Invalid refresh token");
                    return ResponseEntity.status(401).build();
                });
    }

    @PostMapping("/auth/register")
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

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            refreshTokenService.revokeToken(request.getRefreshToken());
        }
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    private com.edutest.persistance.entity.user.UserEntityRole toUserEntityRole(UserRole userRole) {
        if (userRole == null) {
            return com.edutest.persistance.entity.user.UserEntityRole.STUDENT;
        }
        return switch (userRole) {
            case STUDENT -> com.edutest.persistance.entity.user.UserEntityRole.STUDENT;
            case TEACHER -> com.edutest.persistance.entity.user.UserEntityRole.TEACHER;
            case ADMIN -> com.edutest.persistance.entity.user.UserEntityRole.ADMIN;
        };
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
