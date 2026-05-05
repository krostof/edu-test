package com.edutest.webserver.integration;

import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAssignmentEntityEntity;
import com.edutest.persistance.entity.assigment.common.ChoiceOptionEntity;
import com.edutest.persistance.entity.group.StudentGroupEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.repository.TestRepository;
import com.edutest.persistance.repository.StudentGroupJpaRepository;
import com.edutest.persistance.repository.AssignmentJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for navigation restrictions (when allowNavigation=false).
 */
class TestNavigationRestrictionsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private StudentGroupJpaRepository groupRepository;

    @Autowired
    private AssignmentJpaRepository assignmentRepository;

    private TestEntity testEntity;
    private List<SingleChoiceAssignmentEntityEntity> assignments;
    private String studentToken;

    @BeforeEach
    void setUpTestData() throws Exception {
        // Create student group
        StudentGroupEntity studentGroup = StudentGroupEntity.builder()
                .name("Test Group")
                .description("Group for testing")
                .teachers(new ArrayList<>())
                .students(new ArrayList<>())
                .build();
        studentGroup = groupRepository.save(studentGroup);

        // Add student to group
        studentGroup.addStudent(studentUser);
        groupRepository.save(studentGroup);

        // Create test with navigation DISABLED
        testEntity = TestEntity.builder()
                .title("No Navigation Test")
                .description("Test with navigation restrictions")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .timeLimit(60)
                .allowNavigation(false)  // Navigation disabled
                .randomizeOrder(false)
                .createdBy(teacherUser)
                .assignmentEntities(new ArrayList<>())
                .assignedGroups(new ArrayList<>())
                .build();
        testEntity.getAssignedGroups().add(studentGroup);
        testEntity = testRepository.save(testEntity);

        // Create assignments with options
        assignments = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            SingleChoiceAssignmentEntityEntity assignment = new SingleChoiceAssignmentEntityEntity();
            assignment.setTitle("Question " + i);
            assignment.setDescription("Description for question " + i);
            assignment.setOrderNumber(i);
            assignment.setPoints(10);
            assignment.setTestEntity(testEntity);
            assignment.setOptions(new ArrayList<>());

            // Create options before saving
            for (int j = 1; j <= 4; j++) {
                ChoiceOptionEntity option = new ChoiceOptionEntity();
                option.setAssignmentEntity(assignment);
                option.setOptionText("Option " + j);
                option.setIsCorrect(j == 1);
                option.setOrderNumber(j);
                assignment.getOptions().add(option);
            }

            assignment = assignmentRepository.save(assignment);
            testEntity.getAssignmentEntities().add(assignment);
            assignments.add(assignment);
        }

        // Update test with assignments
        testEntity = testRepository.save(testEntity);

        // Get student token
        studentToken = loginAndGetToken(STUDENT_USERNAME);
    }

    @Test
    void shouldAllowMovingForward() throws Exception {
        // Start attempt
        String startResponse = mockMvc.perform(post("/api/tests/{testId}/start", testEntity.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        // Get state - should show allowNavigation=false
        mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/state",
                        testEntity.getId(), attemptId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowNavigation").value(false));

        // Navigate forward should work
        mockMvc.perform(put("/api/tests/{testId}/attempts/{attemptId}/navigation",
                        testEntity.getId(), attemptId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIndex\": 1}"))
                .andExpect(status().isOk());

        // Can get current question
        mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/questions/{index}",
                        testEntity.getId(), attemptId, 1)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionIndex").value(1));
    }

    @Test
    void shouldDenyMovingBackward() throws Exception {
        // Start attempt
        String startResponse = mockMvc.perform(post("/api/tests/{testId}/start", testEntity.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        // Move to question 2
        mockMvc.perform(put("/api/tests/{testId}/attempts/{attemptId}/navigation",
                        testEntity.getId(), attemptId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIndex\": 2}"))
                .andExpect(status().isOk());

        // Try to go back - should be denied
        mockMvc.perform(put("/api/tests/{testId}/attempts/{attemptId}/navigation",
                        testEntity.getId(), attemptId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIndex\": 1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyAccessToPreviousQuestions() throws Exception {
        // Start attempt
        String startResponse = mockMvc.perform(post("/api/tests/{testId}/start", testEntity.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        // Move to question 2
        mockMvc.perform(put("/api/tests/{testId}/attempts/{attemptId}/navigation",
                        testEntity.getId(), attemptId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIndex\": 2}"))
                .andExpect(status().isOk());

        // Try to access previous question - should be denied
        mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/questions/{index}",
                        testEntity.getId(), attemptId, 0)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // But can access current question
        mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/questions/{index}",
                        testEntity.getId(), attemptId, 2)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAnswerToPreviousQuestions() throws Exception {
        // Start attempt
        String startResponse = mockMvc.perform(post("/api/tests/{testId}/start", testEntity.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        // Get assignment order
        String stateResponse = mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/state",
                        testEntity.getId(), attemptId)
                        .header("Authorization", "Bearer " + studentToken))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long firstAssignmentId = objectMapper.readTree(stateResponse)
                .get("assignmentOrder").get(0).asLong();

        Long firstOptionId = assignments.get(0).getOptions().get(0).getId();

        // Move to question 2
        mockMvc.perform(put("/api/tests/{testId}/attempts/{attemptId}/navigation",
                        testEntity.getId(), attemptId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIndex\": 2}"))
                .andExpect(status().isOk());

        // Try to submit answer to first question - should fail (conflict because already moved past)
        mockMvc.perform(post("/api/tests/{testId}/attempts/{attemptId}/answers/{assignmentId}",
                        testEntity.getId(), attemptId, firstAssignmentId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedOptionId\": " + firstOptionId + "}"))
                .andExpect(status().isConflict());
    }
}
