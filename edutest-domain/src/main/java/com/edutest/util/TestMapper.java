package com.edutest.util;

import com.edutest.api.model.Test;
import com.edutest.api.model.TestAttempt;
import com.edutest.api.model.TestDetails;
import com.edutest.api.model.UserProfile;
import com.edutest.domain.user.User;
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
}
