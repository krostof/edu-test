package com.edutest.service.attempt;

import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.assigment.multiplechoice.MultipleChoiceAssignmentEntity;
import com.edutest.persistance.entity.assigment.singlechoice.SingleChoiceAssignmentEntityEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttemptRandomizationService {

    private final ObjectMapper objectMapper;

    public void initializeRandomization(TestAttemptEntity attempt, TestEntity test) {
        List<AssignmentEntity> assignments = test.getAssignmentEntities();

        // Randomize assignment order if enabled
        if (Boolean.TRUE.equals(test.getRandomizeOrder())) {
            List<Long> assignmentIds = assignments.stream()
                    .map(AssignmentEntity::getId)
                    .collect(Collectors.toList());
            Collections.shuffle(assignmentIds);
            attempt.setAssignmentOrder(serializeOrder(assignmentIds));
            log.debug("Randomized assignment order for attempt {}: {}", attempt.getId(), assignmentIds);
        } else {
            // Store original order
            List<Long> assignmentIds = assignments.stream()
                    .sorted(Comparator.comparing(AssignmentEntity::getOrderNumber))
                    .map(AssignmentEntity::getId)
                    .collect(Collectors.toList());
            attempt.setAssignmentOrder(serializeOrder(assignmentIds));
        }

        // Randomize options for choice assignments if enabled
        Map<Long, List<Long>> optionsOrderMap = new HashMap<>();
        for (AssignmentEntity assignment : assignments) {
            if (assignment instanceof SingleChoiceAssignmentEntityEntity singleChoice) {
                if (Boolean.TRUE.equals(singleChoice.getRandomizeOptions())) {
                    List<Long> optionIds = singleChoice.getOptions().stream()
                            .map(o -> o.getId())
                            .collect(Collectors.toList());
                    Collections.shuffle(optionIds);
                    optionsOrderMap.put(assignment.getId(), optionIds);
                    log.debug("Randomized options for single choice assignment {}: {}", assignment.getId(), optionIds);
                }
            } else if (assignment instanceof MultipleChoiceAssignmentEntity multipleChoice) {
                if (Boolean.TRUE.equals(multipleChoice.getRandomizeOptions())) {
                    List<Long> optionIds = multipleChoice.getOptions().stream()
                            .map(o -> o.getId())
                            .collect(Collectors.toList());
                    Collections.shuffle(optionIds);
                    optionsOrderMap.put(assignment.getId(), optionIds);
                    log.debug("Randomized options for multiple choice assignment {}: {}", assignment.getId(), optionIds);
                }
            }
        }

        if (!optionsOrderMap.isEmpty()) {
            attempt.setOptionsOrder(serializeOptionsOrder(optionsOrderMap));
        }
    }

    public List<Long> getAssignmentOrder(TestAttemptEntity attempt) {
        if (attempt.getAssignmentOrder() == null) {
            return Collections.emptyList();
        }
        return deserializeOrder(attempt.getAssignmentOrder());
    }

    public Map<Long, List<Long>> getOptionsOrder(TestAttemptEntity attempt) {
        if (attempt.getOptionsOrder() == null) {
            return Collections.emptyMap();
        }
        return deserializeOptionsOrder(attempt.getOptionsOrder());
    }

    public List<Long> getOptionsOrderForAssignment(TestAttemptEntity attempt, Long assignmentId) {
        Map<Long, List<Long>> optionsOrder = getOptionsOrder(attempt);
        return optionsOrder.getOrDefault(assignmentId, Collections.emptyList());
    }

    private String serializeOrder(List<Long> order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order", e);
            return "[]";
        }
    }

    private List<Long> deserializeOrder(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize order", e);
            return Collections.emptyList();
        }
    }

    private String serializeOptionsOrder(Map<Long, List<Long>> optionsOrder) {
        try {
            return objectMapper.writeValueAsString(optionsOrder);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize options order", e);
            return "{}";
        }
    }

    private Map<Long, List<Long>> deserializeOptionsOrder(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<Long, List<Long>>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize options order", e);
            return Collections.emptyMap();
        }
    }
}
