package com.edutest.webserver.integration;

import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminApiControllerIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("POST /api/admin/students")
    class CreateStudentTests {

        @Test
        @DisplayName("Admin should create student successfully")
        void adminShouldCreateStudent() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            String createJson = """
                {
                    "email": "newstudent@test.com",
                    "password": "Password1!",
                    "firstName": "New",
                    "lastName": "Student",
                    "studentNumber": "STU003"
                }
                """;

            mockMvc.perform(post("/api/admin/students")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("studentn"))
                    .andExpect(jsonPath("$.email").value("newstudent@test.com"))
                    .andExpect(jsonPath("$.firstName").value("New"))
                    .andExpect(jsonPath("$.lastName").value("Student"));
        }

        @Test
        @DisplayName("Teacher should not create student (403)")
        void teacherShouldNotCreateStudent() throws Exception {
            String token = loginAndGetToken(TEACHER_USERNAME);

            String createJson = """
                {
                    "email": "newstudent2@test.com",
                    "password": "Password1!",
                    "firstName": "New",
                    "lastName": "Student",
                    "studentNumber": "STU004"
                }
                """;

            mockMvc.perform(post("/api/admin/students")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Student should not create student (403)")
        void studentShouldNotCreateStudent() throws Exception {
            String token = loginAndGetToken(STUDENT_USERNAME);

            String createJson = """
                {
                    "email": "newstudent3@test.com",
                    "password": "Password1!",
                    "firstName": "New",
                    "lastName": "Student",
                    "studentNumber": "STU005"
                }
                """;

            mockMvc.perform(post("/api/admin/students")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/teachers")
    class CreateTeacherTests {

        @Test
        @DisplayName("Admin should create teacher successfully")
        void adminShouldCreateTeacher() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            String createJson = """
                {
                    "username": "newteacher",
                    "email": "newteacher@test.com",
                    "password": "Password1!",
                    "firstName": "New",
                    "lastName": "Teacher",
                    "employeeId": "EMP001"
                }
                """;

            mockMvc.perform(post("/api/admin/teachers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("newteacher"))
                    .andExpect(jsonPath("$.email").value("newteacher@test.com"));
        }

        @Test
        @DisplayName("Teacher should not create teacher (403)")
        void teacherShouldNotCreateTeacher() throws Exception {
            String token = loginAndGetToken(TEACHER_USERNAME);

            String createJson = """
                {
                    "username": "newteacher2",
                    "email": "newteacher2@test.com",
                    "password": "Password1!",
                    "firstName": "New",
                    "lastName": "Teacher",
                    "employeeId": "EMP002"
                }
                """;

            mockMvc.perform(post("/api/admin/teachers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/users")
    class GetAllUsersTests {

        @Test
        @DisplayName("Admin should get all users")
        void adminShouldGetAllUsers() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(3)))); // admin, teacher, student
        }

        @Test
        @DisplayName("Admin should filter users by role")
        void adminShouldFilterByRole() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + token)
                            .param("role", "STUDENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[*].username", everyItem(not(equalTo("admin")))))
                    .andExpect(jsonPath("$.content[*].username", everyItem(not(equalTo("teacher")))));
        }

        @Test
        @DisplayName("Admin should search users")
        void adminShouldSearchUsers() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + token)
                            .param("search", "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("Admin should paginate users")
        void adminShouldPaginateUsers() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + token)
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))));
        }

        @Test
        @DisplayName("Teacher should not get all users (403)")
        void teacherShouldNotGetAllUsers() throws Exception {
            String token = loginAndGetToken(TEACHER_USERNAME);

            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/users/{userId}")
    class GetUserByIdTests {

        @Test
        @DisplayName("Admin should get user by id")
        void adminShouldGetUserById() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(get("/api/admin/users/" + studentUser.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(studentUser.getId()))
                    .andExpect(jsonPath("$.username").value("student"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent user")
        void shouldReturn404ForNonExistentUser() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(get("/api/admin/users/99999")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/users/{userId}")
    class UpdateUserTests {

        @Test
        @DisplayName("Admin should update user")
        void adminShouldUpdateUser() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            String updateJson = """
                {
                    "firstName": "Updated",
                    "lastName": "Name"
                }
                """;

            mockMvc.perform(put("/api/admin/users/" + studentUser.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Updated"))
                    .andExpect(jsonPath("$.lastName").value("Name"));
        }

        @Test
        @DisplayName("Admin should update user email")
        void adminShouldUpdateUserEmail() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            String updateJson = """
                {
                    "email": "updated@test.com"
                }
                """;

            mockMvc.perform(put("/api/admin/users/" + studentUser.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("updated@test.com"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/users/{userId}")
    class DeleteUserTests {

        @Test
        @DisplayName("Admin should delete user")
        void adminShouldDeleteUser() throws Exception {
            // Create a user to delete
            UserEntity userToDelete = userRepository.save(UserEntity.builder()
                    .username("todelete")
                    .email("todelete@test.com")
                    .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .firstName("To")
                    .lastName("Delete")
                    .roles(new HashSet<>(Set.of(UserEntityRole.STUDENT)))
                    .isActive(true)
                    .build());

            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(delete("/api/admin/users/" + userToDelete.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            // Verify deleted
            mockMvc.perform(get("/api/admin/users/" + userToDelete.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Teacher should not delete user (403)")
        void teacherShouldNotDeleteUser() throws Exception {
            String token = loginAndGetToken(TEACHER_USERNAME);

            mockMvc.perform(delete("/api/admin/users/" + studentUser.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/users/{userId}/activate and /deactivate")
    class ActivateDeactivateTests {

        @Test
        @DisplayName("Admin should deactivate user")
        void adminShouldDeactivateUser() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(patch("/api/admin/users/" + studentUser.getId() + "/deactivate")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive").value(false));
        }

        @Test
        @DisplayName("Admin should activate user")
        void adminShouldActivateUser() throws Exception {
            // First deactivate
            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(patch("/api/admin/users/" + studentUser.getId() + "/deactivate")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // Then activate
            mockMvc.perform(patch("/api/admin/users/" + studentUser.getId() + "/activate")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive").value(true));
        }
    }

    @Nested
    @DisplayName("Batch operations")
    class BatchOperationsTests {

        @Test
        @DisplayName("Admin should batch deactivate users")
        void adminShouldBatchDeactivate() throws Exception {
            // Create users to deactivate
            UserEntity user1 = userRepository.save(UserEntity.builder()
                    .username("batch1")
                    .email("batch1@test.com")
                    .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .firstName("Batch")
                    .lastName("One")
                    .roles(new HashSet<>(Set.of(UserEntityRole.STUDENT)))
                    .isActive(true)
                    .build());

            UserEntity user2 = userRepository.save(UserEntity.builder()
                    .username("batch2")
                    .email("batch2@test.com")
                    .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .firstName("Batch")
                    .lastName("Two")
                    .roles(new HashSet<>(Set.of(UserEntityRole.STUDENT)))
                    .isActive(true)
                    .build());

            String token = loginAndGetToken(ADMIN_USERNAME);

            String batchJson = String.format("""
                {
                    "userIds": [%d, %d]
                }
                """, user1.getId(), user2.getId());

            mockMvc.perform(post("/api/admin/users/batch-deactivate")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(batchJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.successCount").value(2))
                    .andExpect(jsonPath("$.failedCount").value(0));
        }

        @Test
        @DisplayName("Admin should batch delete users")
        void adminShouldBatchDelete() throws Exception {
            // Create users to delete
            UserEntity user1 = userRepository.save(UserEntity.builder()
                    .username("todelete1")
                    .email("todelete1@test.com")
                    .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .firstName("Delete")
                    .lastName("One")
                    .roles(new HashSet<>(Set.of(UserEntityRole.STUDENT)))
                    .isActive(true)
                    .build());

            UserEntity user2 = userRepository.save(UserEntity.builder()
                    .username("todelete2")
                    .email("todelete2@test.com")
                    .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .firstName("Delete")
                    .lastName("Two")
                    .roles(new HashSet<>(Set.of(UserEntityRole.STUDENT)))
                    .isActive(true)
                    .build());

            String token = loginAndGetToken(ADMIN_USERNAME);

            String batchJson = String.format("""
                {
                    "userIds": [%d, %d]
                }
                """, user1.getId(), user2.getId());

            mockMvc.perform(delete("/api/admin/users/batch-delete")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(batchJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.successCount").value(2))
                    .andExpect(jsonPath("$.failedCount").value(0));
        }
    }
}
