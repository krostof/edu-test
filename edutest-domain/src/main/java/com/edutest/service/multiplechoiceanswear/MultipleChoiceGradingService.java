package com.edutest.service.multiplechoiceanswear;

import com.edutest.domain.assignment.common.ChoiceOption;
import com.edutest.domain.assignment.multiplechoice.MultipleChoiceAnswer;
import com.edutest.domain.assignment.multiplechoice.MultipleChoiceAssignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MultipleChoiceGradingService {

    private final MultipleChoiceAnswerService answerService;

    @Autowired
    public MultipleChoiceGradingService(MultipleChoiceAnswerService answerService) {
        this.answerService = answerService;
    }

    public MultipleChoiceAnswer autoGrade(MultipleChoiceAnswer answer,
                                          MultipleChoiceAssignment assignment,
                                          List<ChoiceOption> allOptions) {

        List<ChoiceOption> selectedOptions = answer.getSelectedOptionIds().stream()
                .map(id -> findOptionById(allOptions, id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Oblicz wynik używając logiki assignment
        String answerString = answer.getSelectedOptionIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        Float calculatedScore = assignment.calculateScore(answerString);

        return answer.toBuilder()
                .score(calculatedScore)
                .graded(true)
                .build();
    }

    private ChoiceOption findOptionById(List<ChoiceOption> options, Long id) {
        return options.stream()
                .filter(option -> option.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
