package com.edutest.webserver.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() throws Exception {
            String loginJson = """
                {
                    "username": "admin",
                    "password": "Password1!"
                }
                """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").isNumber())
                    .andExpect(jsonPath("$.user.username").value("admin"))
                    .andExpect(jsonPath("$.user.email").value("admin@test.com"));
        }

        @Test
        @DisplayName("Should return 401 for invalid password")
        void shouldReturn401ForInvalidPassword() throws Exception {
            String loginJson = """
                {
                    "username": "admin",
                    "password": "wrongpassword"
                }
                """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 for non-existent user")
        void shouldReturn401ForNonExistentUser() throws Exception {
            String loginJson = """
                {
                    "username": "nonexistent",
                    "password": "Password1!"
                }
                """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should login as teacher")
        void shouldLoginAsTeacher() throws Exception {
            String loginJson = """
                {
                    "username": "teacher",
                    "password": "Password1!"
                }
                """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.username").value("teacher"));
        }

        @Test
        @DisplayName("Should login as student")
        void shouldLoginAsStudent() throws Exception {
            String loginJson = """
                {
                    "username": "student",
                    "password": "Password1!"
                }
                """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.username").value("student"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUser() throws Exception {
            String registerJson = """
                {
                    "username": "newuser",
                    "email": "newuser@test.com",
                    "password": "Password1!",
                    "firstName": "New",
                    "lastName": "User",
                    "role": "STUDENT",
                    "studentNumber": "STU002"
                }
                """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User registered successfully"))
                    .andExpect(jsonPath("$.user.username").value("newuser"))
                    .andExpect(jsonPath("$.user.email").value("newuser@test.com"));
        }

        @Test
        @DisplayName("Should fail registration with existing username")
        void shouldFailRegistrationWithExistingUsername() throws Exception {
            String registerJson = """
                {
                    "username": "admin",
                    "email": "another@test.com",
                    "password": "Password1!",
                    "firstName": "Another",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail registration with existing email")
        void shouldFailRegistrationWithExistingEmail() throws Exception {
            String registerJson = """
                {
                    "username": "uniqueuser",
                    "email": "admin@test.com",
                    "password": "Password1!",
                    "firstName": "Another",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail registration with invalid email format")
        void shouldFailRegistrationWithInvalidEmail() throws Exception {
            String registerJson = """
                {
                    "username": "newuser2",
                    "email": "invalid-email",
                    "password": "Password1!",
                    "firstName": "New",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail registration with short password")
        void shouldFailRegistrationWithShortPassword() throws Exception {
            String registerJson = """
                {
                    "username": "newuser3",
                    "email": "newuser3@test.com",
                    "password": "short",
                    "firstName": "New",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should register as teacher")
        void shouldRegisterAsTeacher() throws Exception {
            String registerJson = """
                {
                    "username": "newteacher",
                    "email": "newteacher@test.com",
                    "password": "Password1!",
                    "firstName": "New",
                    "lastName": "Teacher",
                    "role": "TEACHER"
                }
                """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.username").value("newteacher"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            // First login to get refresh token
            String loginJson = """
                {
                    "username": "admin",
                    "password": "Password1!"
                }
                """;

            String loginResponse = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

            // Now refresh the token
            String refreshJson = String.format("""
                {
                    "refreshToken": "%s"
                }
                """, refreshToken);

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists());
        }

        @Test
        @DisplayName("Should return 401 for invalid refresh token")
        void shouldReturn401ForInvalidRefreshToken() throws Exception {
            String refreshJson = """
                {
                    "refreshToken": "invalid-token"
                }
                """;

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshJson))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() throws Exception {
            // First login to get refresh token
            String loginJson = """
                {
                    "username": "admin",
                    "password": "Password1!"
                }
                """;

            String loginResponse = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

            // Logout
            String logoutJson = String.format("""
                {
                    "refreshToken": "%s"
                }
                """, refreshToken);

            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(logoutJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));

            // Try to use the refresh token again - should fail
            String refreshJson = String.format("""
                {
                    "refreshToken": "%s"
                }
                """, refreshToken);

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshJson))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should logout without refresh token")
        void shouldLogoutWithoutRefreshToken() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }
    }
}
