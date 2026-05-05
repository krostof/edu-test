package com.edutest.webserver.integration;

import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAssignmentEntityEntity;
import com.edutest.persistance.entity.assigment.common.ChoiceOptionEntity;
import com.edutest.persistance.entity.group.StudentGroupEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.repository.TestRepository;
import com.edutest.persistance.repository.StudentGroupJpaRepository;
import com.edutest.persistance.repository.AssignmentJpaRepository;
import com.edutest.persistance.repository.TestAttemptJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TestAttemptStateIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private StudentGroupJpaRepository groupRepository;

    @Autowired
    private AssignmentJpaRepository assignmentRepository;

    @Autowired
    private TestAttemptJpaRepository attemptRepository;

    
    private TestEntity testEntity;
    private StudentGroupEntity studentGroup;
    private List<SingleChoiceAssignmentEntityEntity> assignments;
    private String studentToken;

    @BeforeEach
    void setUpTestData() throws Exception {
        // Create student group
        studentGroup = StudentGroupEntity.builder()
                .name("Test Group")
                .description("Group for testing")
                .teachers(new ArrayList<>())
                .students(new ArrayList<>())
                .build();
        studentGroup = groupRepository.save(studentGroup);

        // Add student to group
        studentGroup.addStudent(studentUser);
        groupRepository.save(studentGroup);

        // Create test
        testEntity = TestEntity.builder()
                .title("Integration Test")
                .description("Test for integration testing")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .timeLimit(60) // 60 minutes
                .allowNavigation(true)
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

            // Create options before saving assignment
            for (int j = 1; j <= 4; j++) {
                ChoiceOptionEntity option = new ChoiceOptionEntity();
                option.setAssignmentEntity(assignment);
                option.setOptionText("Option " + j);
                option.setIsCorrect(j == 1); // First option is correct
                option.setOrderNumber(j);
                assignment.getOptions().add(option);
            }

            // Save assignment with all options at once (cascade will handle options)
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
    void shouldStartTestAttemptAndGetState() throws Exception {
        // Start attempt
        String startResponse = mockMvc.perform(post("/api/tests/{testId}/start", testEntity.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.testId").value(testEntity.getId()))
                .andExpect(jsonPath("$.isCompleted").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        // Get attempt state
        mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/state", testEntity.getId(), attemptId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId))
                .andExpect(jsonPath("$.testId").value(testEntity.getId()))
                .andExpect(jsonPath("$.testTitle").value("Integration Test"))
                .andExpect(jsonPath("$.currentQuestionIndex").value(0))
                .andExpect(jsonPath("$.totalQuestions").value(3))
                .andExpect(jsonPath("$.remainingTimeSeconds").isNumber())
                .andExpect(jsonPath("$.isCompleted").value(false))
                .andExpect(jsonPath("$.allowNavigation").value(true))
                .andExpect(jsonPath("$.assignmentOrder").isArray())
                .andExpect(jsonPath("$.assignmentOrder", hasSize(3)))
                .andExpect(jsonPath("$.answeredAssignmentIds").isArray())
                .andExpect(jsonPath("$.answeredAssignmentIds", hasSize(0)));
    }

    @Test
    void shouldGetQuestionByIndex() throws Exception {
        // Start attempt
        String startResponse = mockMvc.perform(post("/api/tests/{testId}/start", testEntity.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        // Get first question
        mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/questions/{index}",
                        testEntity.getId(), attemptId, 0)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentId").isNumber())
                .andExpect(jsonPath("$.questionIndex").value(0))
                .andExpect(jsonPath("$.totalQuestions").value(3))
                .andExpect(jsonPath("$.title").value("Question 1"))
                .andExpect(jsonPath("$.assignmentType").value("SINGLE_CHOICE"))
                .andExpect(jsonPath("$.points").value(10))
                .andExpect(jsonPath("$.options").isArray())
                .andExpect(jsonPath("$.options", hasSize(4)))
                .andExpect(jsonPath("$.previousAnswer").doesNotExist());

        // Get second question
        mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/questions/{index}",
                        testEntity.getId(), attemptId, 1)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionIndex").value(1))
                .andExpect(jsonPath("$.title").value("Question 2"));
    }

    @Test
    void shouldUpdateNavigation() throws Exception {
        // Start attempt
        String startResponse = mockMvc.perform(post("/api/tests/{testId}/start", testEntity.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        // Update navigation to question 2
        mockMvc.perform(put("/api/tests/{testId}/attempts/{attemptId}/navigation",
                        testEntity.getId(), attemptId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIndex\": 2}"))
                .andExpect(status().isOk());

        // Verify state shows updated index
        mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/state",
                        testEntity.getId(), attemptId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuestionIndex").value(2));
    }

    @Test
    void shouldReturnPreviousAnswerAfterSubmission() throws Exception {
        // Start attempt
        String startResponse = mockMvc.perform(post("/api/tests/{testId}/start", testEntity.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        // Get assignment ID from state
        String stateResponse = mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/state",
                        testEntity.getId(), attemptId)
                        .header("Authorization", "Bearer " + studentToken))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long firstAssignmentId = objectMapper.readTree(stateResponse)
                .get("assignmentOrder").get(0).asLong();

        // Get first option ID
        Long firstOptionId = assignments.get(0).getOptions().get(0).getId();

        // Submit answer
        mockMvc.perform(post("/api/tests/{testId}/attempts/{attemptId}/answers/{assignmentId}",
                        testEntity.getId(), attemptId, firstAssignmentId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedOptionId\": " + firstOptionId + "}"))
                .andExpect(status().isOk());

        // Get question again - should have previous answer
        mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/questions/{index}",
                        testEntity.getId(), attemptId, 0)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previousAnswer").exists())
                .andExpect(jsonPath("$.previousAnswer.selectedOptionId").value(firstOptionId));

        // Verify answered assignment IDs in state
        mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/state",
                        testEntity.getId(), attemptId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answeredAssignmentIds", hasSize(1)))
                .andExpect(jsonPath("$.answeredAssignmentIds[0]").value(firstAssignmentId));
    }

    @Test
    void shouldDenyAccessToOtherStudentAttempt() throws Exception {
        // Start attempt as student
        String startResponse = mockMvc.perform(post("/api/tests/{testId}/start", testEntity.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        // Try to access as teacher
        String teacherToken = loginAndGetToken(TEACHER_USERNAME);

        mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/state",
                        testEntity.getId(), attemptId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldHandleInvalidQuestionIndex() throws Exception {
        // Start attempt
        String startResponse = mockMvc.perform(post("/api/tests/{testId}/start", testEntity.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long attemptId = objectMapper.readTree(startResponse).get("id").asLong();

        // Try to get question with invalid index
        mockMvc.perform(get("/api/tests/{testId}/attempts/{attemptId}/questions/{index}",
                        testEntity.getId(), attemptId, 999)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());
    }
}
