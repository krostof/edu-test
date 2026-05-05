package com.edutest.util;

import com.edutest.api.model.*;
import com.edutest.domain.user.User;
import com.edutest.dto.PreviousAnswerDto;
import com.edutest.dto.QuestionOptionDto;
import com.edutest.dto.QuestionViewDto;
import com.edutest.dto.TestAttemptStateDto;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TestMapper {

    private final UserMapper userMapper;
    private final AssignmentMapper assignmentMapper;
    private final UserRepository userRepository;

    /**
     * Maps a TestEntity to a domain Test object.
     */
    public com.edutest.domain.test.Test toDomain(TestEntity entity) {
        if (entity == null) {
            return null;
        }

        com.edutest.domain.test.Test test = com.edutest.domain.test.Test.builder()
                .title(entity.getTitle())
                .description(entity.getDescription())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .timeLimit(entity.getTimeLimit())
                .allowNavigation(entity.getAllowNavigation())
                .randomizeOrder(entity.getRandomizeOrder())
                .createdBy(entity.getCreatedBy() != null ? userMapper.toUser(entity.getCreatedBy()) : null)
                .assignedGroups(new ArrayList<>())
                .assignments(new ArrayList<>())
                .attempts(new ArrayList<>())
                .build();
        test.setId(entity.getId());
        test.setCreatedAt(entity.getCreatedAt());
        test.setUpdatedAt(entity.getUpdatedAt());
        return test;
    }

    /**
     * Maps a domain Test to a TestEntity.
     */
    public TestEntity toEntity(com.edutest.domain.test.Test domain, UserEntity createdByEntity) {
        if (domain == null) {
            return null;
        }

        TestEntity entity = TestEntity.builder()
                .title(domain.getTitle())
                .description(domain.getDescription())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .timeLimit(domain.getTimeLimit())
                .allowNavigation(domain.getAllowNavigation())
                .randomizeOrder(domain.getRandomizeOrder())
                .createdBy(createdByEntity)
                .build();
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }

    public Test toApiTest(com.edutest.domain.test.Test domain) {
        Test api = new Test();
        api.setId(domain.getId());
        api.setTitle(domain.getTitle());
        api.setDescription(domain.getDescription());
        if (domain.getStartDate() != null) {
            api.setStartDate(domain.getStartDate().atOffset(ZoneOffset.UTC));
        }
        if (domain.getEndDate() != null) {
            api.setEndDate(domain.getEndDate().atOffset(ZoneOffset.UTC));
        }
        api.setTimeLimit(domain.getTimeLimit());
        api.setAllowNavigation(domain.getAllowNavigation());
        api.setRandomizeOrder(domain.getRandomizeOrder());

        if (domain.getCreatedBy() != null) {
            api.setCreatedBy(toUserProfile(domain.getCreatedBy()));
        }

        return api;
    }

    public TestDetails toApiTestDetails(com.edutest.domain.test.Test domain) {
        TestDetails details = new TestDetails();
        details.setId(domain.getId());
        details.setTitle(domain.getTitle());
        details.setDescription(domain.getDescription());
        if (domain.getStartDate() != null) {
            details.setStartDate(domain.getStartDate().atOffset(ZoneOffset.UTC));
        }
        if (domain.getEndDate() != null) {
            details.setEndDate(domain.getEndDate().atOffset(ZoneOffset.UTC));
        }
        details.setTimeLimit(domain.getTimeLimit());
        details.setAllowNavigation(domain.getAllowNavigation());
        details.setRandomizeOrder(domain.getRandomizeOrder());

        if (domain.getCreatedBy() != null) {
            details.setCreatedBy(toUserProfile(domain.getCreatedBy()));
        }

        if (domain.getAssignments() != null) {
            details.setAssignments(domain.getAssignments().stream()
                    .map(assignmentMapper::toApiAssignment)
                    .collect(Collectors.toList()));
        }

        return details;
    }

    public TestAttempt toApiTestAttempt(com.edutest.domain.test.TestAttempt domain) {
        TestAttempt api = new TestAttempt();
        api.setId(domain.getId());
        if (domain.getTest() != null) {
            api.setTestId(domain.getTest().getId());
        }
        if (domain.getStudent() != null) {
            api.setStudentId(domain.getStudent().getId());
        }
        if (domain.getStartedAt() != null) {
            api.setStartedAt(domain.getStartedAt().atOffset(ZoneOffset.UTC));
        }
        if (domain.getFinishedAt() != null) {
            api.setFinishedAt(domain.getFinishedAt().atOffset(ZoneOffset.UTC));
        }
        api.setScore(domain.getScore());
        api.setIsCompleted(domain.getIsCompleted());
        return api;
    }

    public TestAttempt toApiTestAttempt(TestAttemptEntity entity) {
        TestAttempt api = new TestAttempt();
        api.setId(entity.getId());
        if (entity.getTestEntity() != null) {
            api.setTestId(entity.getTestEntity().getId());
        }
        if (entity.getStudent() != null) {
            api.setStudentId(entity.getStudent().getId());
        }
        if (entity.getStartedAt() != null) {
            api.setStartedAt(entity.getStartedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getFinishedAt() != null) {
            api.setFinishedAt(entity.getFinishedAt().atOffset(ZoneOffset.UTC));
        }
        api.setScore(entity.getScore());
        api.setIsCompleted(entity.getIsCompleted());
        return api;
    }

    private UserProfile toUserProfile(User user) {
        UserProfile profile = new UserProfile();
        profile.setUsername(user.getUsername());
        profile.setEmail(user.getEmail());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());

        // Retrieve full entity to get id
        userRepository.findByUsername(user.getUsername()).ifPresent(entity -> {
            profile.setId(entity.getId());
            profile.setIsActive(entity.getIsActive());
        });

        return profile;
    }

    public TestAttemptState toApiAttemptState(TestAttemptStateDto dto) {
        TestAttemptState api = new TestAttemptState();
        api.setAttemptId(dto.getAttemptId());
        api.setTestId(dto.getTestId());
        api.setTestTitle(dto.getTestTitle());
        api.setCurrentQuestionIndex(dto.getCurrentQuestionIndex());
        api.setTotalQuestions(dto.getTotalQuestions());
        api.setRemainingTimeSeconds(dto.getRemainingTimeSeconds());
        if (dto.getStartedAt() != null) {
            api.setStartedAt(dto.getStartedAt().atOffset(ZoneOffset.UTC));
        }
        api.setIsCompleted(dto.getIsCompleted());
        api.setAllowNavigation(dto.getAllowNavigation());
        api.setAssignmentOrder(dto.getAssignmentOrder());
        api.setAnsweredAssignmentIds(dto.getAnsweredAssignmentIds());
        return api;
    }

    public QuestionView toApiQuestionView(QuestionViewDto dto) {
        QuestionView api = new QuestionView();
        api.setAssignmentId(dto.getAssignmentId());
        api.setQuestionIndex(dto.getQuestionIndex());
        api.setTotalQuestions(dto.getTotalQuestions());
        api.setTitle(dto.getTitle());
        api.setDescription(dto.getDescription());
        if (dto.getAssignmentType() != null) {
            api.setAssignmentType(QuestionView.AssignmentTypeEnum.fromValue(dto.getAssignmentType()));
        }
        api.setPoints(dto.getPoints());
        api.setProgrammingLanguage(dto.getProgrammingLanguage());
        api.setStarterCode(dto.getStarterCode());

        if (dto.getOptions() != null) {
            api.setOptions(dto.getOptions().stream()
                    .map(this::toApiQuestionOption)
                    .collect(Collectors.toList()));
        }

        if (dto.getPreviousAnswer() != null) {
            api.setPreviousAnswer(toApiPreviousAnswer(dto.getPreviousAnswer()));
        }

        return api;
    }

    private QuestionOption toApiQuestionOption(QuestionOptionDto dto) {
        QuestionOption api = new QuestionOption();
        api.setId(dto.getId());
        api.setText(dto.getText());
        api.setOrderNumber(dto.getOrderNumber());
        return api;
    }

    private PreviousAnswer toApiPreviousAnswer(PreviousAnswerDto dto) {
        PreviousAnswer api = new PreviousAnswer();
        api.setSelectedOptionId(dto.getSelectedOptionId());
        api.setSelectedOptionIds(dto.getSelectedOptionIds());
        api.setAnswerText(dto.getAnswerText());
        api.setSourceCode(dto.getSourceCode());
        api.setProgrammingLanguage(dto.getProgrammingLanguage());
        if (dto.getAnsweredAt() != null) {
            api.setAnsweredAt(dto.getAnsweredAt().atOffset(ZoneOffset.UTC));
        }
        return api;
    }
}
