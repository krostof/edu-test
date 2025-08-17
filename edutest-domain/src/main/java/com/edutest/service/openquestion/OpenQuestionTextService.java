package com.edutest.service.openquestion;

import com.edutest.domain.assignment.openquestion.OpenQuestionAnswer;

public class OpenQuestionTextService {

    public OpenQuestionAnswer updateCounts(OpenQuestionAnswer answer) {
        if (answer.getAnswerText() != null) {
            int characterCount = answer.getAnswerText().length();
            int wordCount = countWords(answer.getAnswerText());

            return answer.toBuilder()
                    .characterCount(characterCount)
                    .wordCount(wordCount)
                    .build();
        } else {
            return answer.toBuilder()
                    .characterCount(0)
                    .wordCount(0)
                    .build();
        }
    }

    public String createPreview(String text, int maxLength) {
        if (text == null) return "";

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + "...";
    }

    public String createPreview(OpenQuestionAnswer answer) {
        return createPreview(answer.getAnswerText(), 100);
    }

    public String createPreview(OpenQuestionAnswer answer, int maxLength) {
        return createPreview(answer.getAnswerText(), maxLength);
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
