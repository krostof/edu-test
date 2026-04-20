package com.edutest.webserver.integration;

import com.edutest.persistance.entity.group.StudentGroupEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.persistance.repository.StudentGroupJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GroupsApiControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private StudentGroupJpaRepository studentGroupJpaRepository;

    private StudentGroupEntity testGroup;
    private UserEntity additionalTeacher;
    private UserEntity additionalStudent;

    @BeforeEach
    void setUpGroupData() {
        studentGroupJpaRepository.deleteAll();

        // Create additional teacher
        additionalTeacher = userRepository.save(UserEntity.builder()
                .username("teacher2")
                .email("teacher2@test.com")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .firstName("Second")
                .lastName("Teacher")
                .roles(Set.of(UserEntityRole.TEACHER))
                .isActive(true)
                .build());

        // Create additional student
        additionalStudent = userRepository.save(UserEntity.builder()
                .username("student2")
                .email("student2@test.com")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .firstName("Second")
                .lastName("Student")
                .roles(Set.of(UserEntityRole.STUDENT))
                .isActive(true)
                .studentNumber("STU002")
                .build());

        // Create test group with teacher
        testGroup = new StudentGroupEntity();
        testGroup.setName("Test Group");
        testGroup.setDescription("A test group");
        testGroup.setTeachers(new ArrayList<>());
        testGroup.getTeachers().add(teacherUser);
        testGroup = studentGroupJpaRepository.save(testGroup);
    }

    @Nested
    @DisplayName("GET /api/groups")
    class GetGroupsTests {

        @Test
        @DisplayName("Admin should see all groups")
        void adminShouldSeeAllGroups() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(get("/api/groups")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].name").value("Test Group"));
        }

        @Test
        @DisplayName("Teacher should see their managed groups")
        void teacherShouldSeeTheirGroups() throws Exception {
            String token = loginAndGetToken(TEACHER_USERNAME);

            mockMvc.perform(get("/api/groups")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Test Group"));
        }

        @Test
        @DisplayName("Student should see their group or empty list")
        void studentShouldSeeTheirGroupOrEmpty() throws Exception {
            String token = loginAndGetToken(STUDENT_USERNAME);

            mockMvc.perform(get("/api/groups")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0))); // Student not in any group yet
        }

        @Test
        @DisplayName("Unauthenticated request should return 401")
        void unauthenticatedShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/groups"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/groups")
    class CreateGroupTests {

        @Test
        @DisplayName("Admin should create group successfully")
        void adminShouldCreateGroup() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            String createJson = String.format("""
                {
                    "name": "New Group",
                    "description": "A new group",
                    "teacherIds": [%d]
                }
                """, teacherUser.getId());

            mockMvc.perform(post("/api/groups")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("New Group"))
                    .andExpect(jsonPath("$.description").value("A new group"));
        }

        @Test
        @DisplayName("Admin should create group without teachers")
        void adminShouldCreateGroupWithoutTeachers() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            String createJson = """
                {
                    "name": "Empty Group",
                    "description": "A group without teachers"
                }
                """;

            mockMvc.perform(post("/api/groups")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Empty Group"));
        }

        @Test
        @DisplayName("Teacher should not create group (403)")
        void teacherShouldNotCreateGroup() throws Exception {
            String token = loginAndGetToken(TEACHER_USERNAME);

            String createJson = """
                {
                    "name": "Unauthorized Group",
                    "description": "Should fail"
                }
                """;

            mockMvc.perform(post("/api/groups")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Student should not create group (403)")
        void studentShouldNotCreateGroup() throws Exception {
            String token = loginAndGetToken(STUDENT_USERNAME);

            String createJson = """
                {
                    "name": "Unauthorized Group",
                    "description": "Should fail"
                }
                """;

            mockMvc.perform(post("/api/groups")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should fail to create group with duplicate name")
        void shouldFailWithDuplicateName() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            String createJson = """
                {
                    "name": "Test Group",
                    "description": "Duplicate name"
                }
                """;

            mockMvc.perform(post("/api/groups")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/groups/{groupId}")
    class GetGroupByIdTests {

        @Test
        @DisplayName("Should get group details")
        void shouldGetGroupDetails() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(get("/api/groups/" + testGroup.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testGroup.getId()))
                    .andExpect(jsonPath("$.name").value("Test Group"))
                    .andExpect(jsonPath("$.description").value("A test group"));
        }

        @Test
        @DisplayName("Should return 400 for non-existent group")
        void shouldReturn400ForNonExistentGroup() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(get("/api/groups/99999")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/groups/{groupId}")
    class UpdateGroupTests {

        @Test
        @DisplayName("Admin should update group")
        void adminShouldUpdateGroup() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            String updateJson = """
                {
                    "name": "Updated Group Name",
                    "description": "Updated description"
                }
                """;

            mockMvc.perform(put("/api/groups/" + testGroup.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Group Name"))
                    .andExpect(jsonPath("$.description").value("Updated description"));
        }

        @Test
        @DisplayName("Teacher should not update group (403)")
        void teacherShouldNotUpdateGroup() throws Exception {
            String token = loginAndGetToken(TEACHER_USERNAME);

            String updateJson = """
                {
                    "name": "Unauthorized Update"
                }
                """;

            mockMvc.perform(put("/api/groups/" + testGroup.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/groups/{groupId}")
    class DeleteGroupTests {

        @Test
        @DisplayName("Admin should delete group")
        void adminShouldDeleteGroup() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(delete("/api/groups/" + testGroup.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            // Verify it's deleted
            mockMvc.perform(get("/api/groups/" + testGroup.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Teacher should not delete group (403)")
        void teacherShouldNotDeleteGroup() throws Exception {
            String token = loginAndGetToken(TEACHER_USERNAME);

            mockMvc.perform(delete("/api/groups/" + testGroup.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Teacher management")
    class TeacherManagementTests {

        @Test
        @DisplayName("Admin should add teacher to group")
        void adminShouldAddTeacher() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            String addTeacherJson = String.format("""
                {
                    "teacherId": %d
                }
                """, additionalTeacher.getId());

            mockMvc.perform(post("/api/groups/" + testGroup.getId() + "/teachers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addTeacherJson))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin should remove teacher from group")
        void adminShouldRemoveTeacher() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            mockMvc.perform(delete("/api/groups/" + testGroup.getId() + "/teachers/" + teacherUser.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Teacher should not add teacher (403)")
        void teacherShouldNotAddTeacher() throws Exception {
            String token = loginAndGetToken(TEACHER_USERNAME);

            String addTeacherJson = String.format("""
                {
                    "teacherId": %d
                }
                """, additionalTeacher.getId());

            mockMvc.perform(post("/api/groups/" + testGroup.getId() + "/teachers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addTeacherJson))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Student management")
    class StudentManagementTests {

        @Test
        @DisplayName("Admin should add student to group")
        void adminShouldAddStudent() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            String addStudentJson = String.format("""
                {
                    "studentId": %d
                }
                """, studentUser.getId());

            mockMvc.perform(post("/api/groups/" + testGroup.getId() + "/students")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addStudentJson))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin should add multiple students to group")
        void adminShouldAddMultipleStudents() throws Exception {
            String token = loginAndGetToken(ADMIN_USERNAME);

            String addStudentsJson = String.format("""
                {
                    "studentIds": [%d, %d]
                }
                """, studentUser.getId(), additionalStudent.getId());

            mockMvc.perform(post("/api/groups/" + testGroup.getId() + "/students/batch")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addStudentsJson))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin should remove student from group")
        void adminShouldRemoveStudent() throws Exception {
            // First add student
            String tokenForAdd = loginAndGetToken(ADMIN_USERNAME);
            String addStudentJson = String.format("""
                {
                    "studentId": %d
                }
                """, studentUser.getId());

            mockMvc.perform(post("/api/groups/" + testGroup.getId() + "/students")
                            .header("Authorization", "Bearer " + tokenForAdd)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addStudentJson))
                    .andExpect(status().isOk());

            // Then remove
            String token = loginAndGetToken(ADMIN_USERNAME);
            mockMvc.perform(delete("/api/groups/" + testGroup.getId() + "/students/" + studentUser.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Teacher should not add student (403)")
        void teacherShouldNotAddStudent() throws Exception {
            String token = loginAndGetToken(TEACHER_USERNAME);

            String addStudentJson = String.format("""
                {
                    "studentId": %d
                }
                """, studentUser.getId());

            mockMvc.perform(post("/api/groups/" + testGroup.getId() + "/students")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addStudentJson))
                    .andExpect(status().isForbidden());
        }
    }
}
