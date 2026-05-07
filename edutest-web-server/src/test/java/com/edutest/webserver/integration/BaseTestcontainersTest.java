package com.edutest.webserver.integration;

import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.persistance.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for integration tests using PostgreSQL Testcontainers.
 *
 * Requires Docker to be running. For tests without Docker, extend BaseIntegrationTest instead.
 *
 * Usage: Extend this class instead of BaseIntegrationTest when you need
 * real PostgreSQL behavior in your tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public abstract class BaseTestcontainersTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("edutest_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        // Disable Flyway in integration tests — Hibernate handles schema, V1 baseline is empty.
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("app.jwtSecret", () -> "testSecretKeyForJwtTokenGenerationInTestsWithAtLeast256Bits1234567890");
        registry.add("app.jwtExpirationMs", () -> "86400000");
        registry.add("app.refreshTokenExpirationMs", () -> "604800000");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected UserEntity adminUser;
    protected UserEntity teacherUser;
    protected UserEntity studentUser;

    protected static final String ADMIN_USERNAME = "admin";
    protected static final String TEACHER_USERNAME = "teacher";
    protected static final String STUDENT_USERNAME = "student";
    protected static final String DEFAULT_PASSWORD = "Password1!";

    @BeforeEach
    void setUpBaseData() {
        // Clear all data before each test
        userRepository.deleteAll();

        // Create admin user
        adminUser = userRepository.save(UserEntity.builder()
                .username(ADMIN_USERNAME)
                .email("admin@test.com")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .firstName("Admin")
                .lastName("User")
                .roles(new HashSet<>(Set.of(UserEntityRole.ADMIN)))
                .isActive(true)
                .build());

        // Create teacher user
        teacherUser = userRepository.save(UserEntity.builder()
                .username(TEACHER_USERNAME)
                .email("teacher@test.com")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .firstName("Teacher")
                .lastName("User")
                .roles(new HashSet<>(Set.of(UserEntityRole.TEACHER)))
                .isActive(true)
                .build());

        // Create student user
        studentUser = userRepository.save(UserEntity.builder()
                .username(STUDENT_USERNAME)
                .email("student@test.com")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .firstName("Student")
                .lastName("User")
                .roles(new HashSet<>(Set.of(UserEntityRole.STUDENT)))
                .isActive(true)
                .studentNumber("STU001")
                .build());
    }

    protected String loginAndGetToken(String username) throws Exception {
        String loginJson = String.format("""
            {
                "username": "%s",
                "password": "%s"
            }
            """, username, DEFAULT_PASSWORD);

        String response = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
