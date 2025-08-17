package com.edutest.persistance.entity.assigment.openquestion;

import com.edutest.persistance.entity.assigment.common.AssignmentAnswerEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("OPEN_QUESTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenQuestionAnswerEntity extends AssignmentAnswerEntity {

    @Column(name = "answer_text", length = 10000)
    private String answerText;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "character_count")
    private Integer characterCount;

    @PreUpdate
    private void updateCounts() {
        if (answerText != null) {
            this.characterCount = answerText.length();
            this.wordCount = countWords(answerText);
        } else {
            this.characterCount = 0;
            this.wordCount = 0;
        }
    }

    @Override
    public boolean isCorrect() {
        return getScore() != null && getScore() > 0;
    }

    @Override
    public float calculateScore() {
        OpenQuestionAssignmentEntityEntity assignment = (OpenQuestionAssignmentEntityEntity) getAssignmentEntity();
        return assignment.calculateScore(answerText);
    }

    @Override
    public String getAnswerText() {
        return answerText;
    }

    public boolean isEmpty() {
        return answerText == null || answerText.trim().isEmpty();
    }

    public boolean meetsMinimumLength() {
        OpenQuestionAssignmentEntityEntity assignment = (OpenQuestionAssignmentEntityEntity) getAssignmentEntity();
        Integer minLength = assignment.getMinLength();

        if (minLength == null) {
            return true;
        }

        return characterCount != null && characterCount >= minLength;
    }

    public boolean exceedsMaximumLength() {
        OpenQuestionAssignmentEntityEntity assignment = (OpenQuestionAssignmentEntityEntity) getAssignmentEntity();
        Integer maxLength = assignment.getMaxLength();

        if (maxLength == null) {
            return false;
        }

        return characterCount != null && characterCount > maxLength;
    }

    public boolean isWithinLengthLimits() {
        return meetsMinimumLength() && !exceedsMaximumLength();
    }

    public String getLengthStatus() {
        OpenQuestionAssignmentEntityEntity assignment = (OpenQuestionAssignmentEntityEntity) getAssignmentEntity();

        if (isEmpty()) {
            return "No answer provided";
        }

        if (!meetsMinimumLength()) {
            return String.format("Too short (minimum: %d characters)", assignment.getMinLength());
        }

        if (exceedsMaximumLength()) {
            return String.format("Too long (maximum: %d characters)", assignment.getMaxLength());
        }

        return "Within length limits";
    }

    public int getRemainingCharacters() {
        OpenQuestionAssignmentEntityEntity assignment = (OpenQuestionAssignmentEntityEntity) getAssignmentEntity();
        Integer maxLength = assignment.getMaxLength();

        if (maxLength == null) {
            return -1; // No limit
        }

        int currentLength = characterCount != null ? characterCount : 0;
        return Math.max(0, maxLength - currentLength);
    }

    public int getRequiredCharacters() {
        OpenQuestionAssignmentEntityEntity assignment = (OpenQuestionAssignmentEntityEntity) getAssignmentEntity();
        Integer minLength = assignment.getMinLength();

        if (minLength == null) {
            return 0;
        }

        int currentLength = characterCount != null ? characterCount : 0;
        return Math.max(0, minLength - currentLength);
    }

    public float getCompletionPercentage() {
        OpenQuestionAssignmentEntityEntity assignment = (OpenQuestionAssignmentEntityEntity) getAssignmentEntity();
        Integer minLength = assignment.getMinLength();

        if (minLength == null || minLength == 0) {
            return answerText != null && !answerText.trim().isEmpty() ? 100.0f : 0.0f;
        }

        int currentLength = characterCount != null ? characterCount : 0;
        return Math.min(100.0f, (float) currentLength / minLength * 100.0f);
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        return text.trim().split("\\s+").length;
    }

    public String getAnswerPreview(int maxLength) {
        if (answerText == null) {
            return "";
        }

        if (answerText.length() <= maxLength) {
            return answerText;
        }

        return answerText.substring(0, maxLength) + "...";
    }

    public String getAnswerPreview() {
        return getAnswerPreview(100); // Default preview length
    }
}
