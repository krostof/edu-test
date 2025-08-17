package com.edutest.service.multiplechoiceanswear;

import com.edutest.domain.assignment.multiplechoice.MultipleChoiceAnswer;
import lombok.Builder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MultipleChoiceSelectionService {

    public MultipleChoiceAnswer selectOption(MultipleChoiceAnswer answer, Long optionId) {
        if (optionId == null) {
            throw new IllegalArgumentException("Option ID cannot be null");
        }

        if (!answer.isOptionSelected(optionId)) {
            List<Long> newSelection = new ArrayList<>(answer.getSelectedOptionIds());
            newSelection.add(optionId);

            return answer.builder()
                    .selectedOptionIds(newSelection)
                    .answeredAt(LocalDateTime.now())
                    .build();
        }

        return answer;
    }

    public MultipleChoiceAnswer deselectOption(MultipleChoiceAnswer answer, Long optionId) {
        if (optionId == null) {
            return answer;
        }

        List<Long> newSelection = new ArrayList<>(answer.getSelectedOptionIds());
        newSelection.remove(optionId);

        LocalDateTime answeredAt = newSelection.isEmpty() ? null : answer.getAnsweredAt();

        return answer.builder()
                .selectedOptionIds(newSelection)
                .answeredAt(answeredAt)
                .build();
    }

    public MultipleChoiceAnswer clearAllSelections(MultipleChoiceAnswer answer) {
        return answer.builder()
                .selectedOptionIds(new ArrayList<>())
                .answeredAt(null)
                .build();
    }

    public MultipleChoiceAnswer setSelectedOptions(MultipleChoiceAnswer answer, List<Long> optionIds) {
        List<Long> newSelection = optionIds != null ? new ArrayList<>(optionIds) : new ArrayList<>();
        LocalDateTime answeredAt = newSelection.isEmpty() ? null : LocalDateTime.now();

        return answer.builder()
                .selectedOptionIds(newSelection)
                .answeredAt(answeredAt)
                .build();
    }
}
