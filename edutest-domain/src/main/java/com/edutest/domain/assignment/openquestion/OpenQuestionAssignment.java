package com.edutest.domain.assignment.openquestion;

import com.edutest.domain.assignment.Assignment;
import com.edutest.domain.assignment.AssignmentType;
import com.edutest.domain.assignment.ValidationResult;
import jakarta.persistence.DiscriminatorValue;
import lombok.*;

@DiscriminatorValue("OPEN_QUESTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenQuestionAssignment extends Assignment {

    private Integer maxLength;

    private Integer minLength;

    private String sampleAnswer;

    private String gradingRubric;

    private Boolean allowHtml = false;

    private Boolean caseSensitive = false;

    @Override
    public AssignmentType getType() {
        return AssignmentType.OPEN_QUESTION;
    }

    @Override
    public ValidationResult validateAnswer(String answer) {
        return null;
    }


    public boolean isValidAnswer(String answer) {
        if (answer == null) {
            return false;
        }

        String trimmedAnswer = answer.trim();

        if (minLength != null && trimmedAnswer.length() < minLength) {
            return false;
        }

        if (maxLength != null && trimmedAnswer.length() > maxLength) {
            return false;
        }

        return !trimmedAnswer.isEmpty();
    }

    @Override
    public Float calculateScore(String answer) {

        if (!isValidAnswer(answer)) {
            return 0.0f;
        }

        if (sampleAnswer != null && !sampleAnswer.trim().isEmpty()) {
            return calculateBasicSimilarityScore(answer, sampleAnswer);
        }

        return getPoints();
    }

    @Override
    public boolean supportsAttachments() {
        return false;
    }

    private float calculateBasicSimilarityScore(String studentAnswer, String sampleAnswer) {
        if (studentAnswer == null || sampleAnswer == null) {
            return 0.0f;
        }

        String student = caseSensitive ? studentAnswer.trim() : studentAnswer.trim().toLowerCase();
        String sample = caseSensitive ? sampleAnswer.trim() : sampleAnswer.trim().toLowerCase();

        if (student.equals(sample)) {
            return getPoints();
        }

        String[] sampleWords = sample.split("\\s+");
        String[] studentWords = student.split("\\s+");

        long matchingWords = 0;
        for (String sampleWord : sampleWords) {
            for (String studentWord : studentWords) {
                if (sampleWord.equals(studentWord)) {
                    matchingWords++;
                    break;
                }
            }
        }

        if (sampleWords.length == 0) {
            return 0.0f;
        }

        float similarity = (float) matchingWords / sampleWords.length;
        return similarity * getPoints();
    }

    public boolean requiresManualGrading() {
        return true;
    }

    public boolean hasLengthRestrictions() {
        return minLength != null || maxLength != null;
    }

    public String getLengthRequirements() {
        if (!hasLengthRestrictions()) {
            return "No length restrictions";
        }

        StringBuilder sb = new StringBuilder();
        if (minLength != null && maxLength != null) {
            sb.append("Between ").append(minLength).append(" and ").append(maxLength).append(" characters");
        } else if (minLength != null) {
            sb.append("At least ").append(minLength).append(" characters");
        } else if (maxLength != null) {
            sb.append("At most ").append(maxLength).append(" characters");
        }

        return sb.toString();
    }

    public boolean isAnswerWithinLimits(String answer) {
        if (answer == null) {
            return minLength == null || minLength == 0;
        }

        int length = answer.trim().length();

        if (minLength != null && length < minLength) {
            return false;
        }

        if (maxLength != null && length > maxLength) {
            return false;
        }

        return true;
    }

    public int getRemainingCharacters(String currentAnswer) {
        if (maxLength == null) {
            return -1; // No limit
        }

        int currentLength = currentAnswer != null ? currentAnswer.length() : 0;
        return Math.max(0, maxLength - currentLength);
    }

    public int getRequiredCharacters(String currentAnswer) {
        if (minLength == null) {
            return 0;
        }

        int currentLength = currentAnswer != null ? currentAnswer.trim().length() : 0;
        return Math.max(0, minLength - currentLength);
    }
}
