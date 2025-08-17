package com.edutest.persistance.entity.assigment.openquestion;

import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.assigment.AssignmentType;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Entity
@DiscriminatorValue("OPEN_QUESTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenQuestionAssignmentEntityEntity extends AssignmentEntity {

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "min_length")
    private Integer minLength;

    @Column(name = "sample_answer", length = 5000)
    private String sampleAnswer;

    @Column(name = "grading_rubric", length = 2000)
    private String gradingRubric;

    @Column(name = "allow_html")
    @Builder.Default
    private Boolean allowHtml = false;

    @Column(name = "case_sensitive")
    @Builder.Default
    private Boolean caseSensitive = false;

    @Override
    public AssignmentType getType() {
        return AssignmentType.OPEN_QUESTION;
    }

    @Override
    public boolean isValidAnswer(String answer) {
        if (answer == null) {
            return false;
        }

        String trimmedAnswer = answer.trim();

        // Check minimum length
        if (minLength != null && trimmedAnswer.length() < minLength) {
            return false;
        }


        if (maxLength != null && trimmedAnswer.length() > maxLength) {
            return false;
        }


        return !trimmedAnswer.isEmpty();
    }

    @Override
    public float calculateScore(String answer) {


        if (!isValidAnswer(answer)) {
            return 0.0f;
        }

        // Basic automatic scoring - can be enhanced with keyword matching, etc.
        if (sampleAnswer != null && !sampleAnswer.trim().isEmpty()) {
            return calculateBasicSimilarityScore(answer, sampleAnswer);
        }

        // Return full points if no sample answer - requires manual grading
        return getPoints();
    }

    private float calculateBasicSimilarityScore(String studentAnswer, String sampleAnswer) {
        if (studentAnswer == null || sampleAnswer == null) {
            return 0.0f;
        }

        String student = caseSensitive ? studentAnswer.trim() : studentAnswer.trim().toLowerCase();
        String sample = caseSensitive ? sampleAnswer.trim() : sampleAnswer.trim().toLowerCase();

        // Very basic similarity - exact match
        if (student.equals(sample)) {
            return getPoints();
        }

        // Partial credit for containing key words (basic implementation)
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

    // Business methods
    public boolean requiresManualGrading() {
        return true; // Open questions typically need teacher review
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
