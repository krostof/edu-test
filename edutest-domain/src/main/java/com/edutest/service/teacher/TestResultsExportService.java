package com.edutest.service.teacher;

import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.assigment.AssignmentType;
import com.edutest.persistance.entity.assigment.common.AssignmentAnswerEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestResultsExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TestRepository testRepository;
    private final TestAttemptJpaRepository testAttemptRepository;
    private final AssignmentJpaRepository assignmentRepository;
    private final AssignmentAnswerJpaRepository answerRepository;
    private final CodeSubmissionJpaRepository codeSubmissionRepository;

    @Transactional(readOnly = true)
    public String exportToCsv(Long testId) {
        TestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found"));

        List<AssignmentEntity> assignments = assignmentRepository.findByTestEntityIdOrderByOrderNumber(testId);
        List<TestAttemptEntity> attempts = testAttemptRepository.findByTestId(testId);

        // Filter to only completed attempts
        attempts = attempts.stream()
                .filter(TestAttemptEntity::isFinished)
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();

        // Header row
        csv.append(escapeCSV("Student Name"));
        csv.append(",").append(escapeCSV("Student Email"));
        csv.append(",").append(escapeCSV("Student Number"));
        csv.append(",").append(escapeCSV("Group"));

        // Assignment columns
        for (AssignmentEntity assignment : assignments) {
            String header = String.format("%s (max: %d)", assignment.getTitle(), assignment.getPoints());
            csv.append(",").append(escapeCSV(header));
        }

        csv.append(",").append(escapeCSV("Total Score"));
        csv.append(",").append(escapeCSV("Max Possible"));
        csv.append(",").append(escapeCSV("Percentage"));
        csv.append(",").append(escapeCSV("Status"));
        csv.append(",").append(escapeCSV("Started At"));
        csv.append(",").append(escapeCSV("Finished At"));
        csv.append("\n");

        // Calculate max possible score
        Float maxPossible = assignmentRepository.sumPointsByTestId(testId);
        if (maxPossible == null) maxPossible = 0f;

        // Data rows
        for (TestAttemptEntity attempt : attempts) {
            UserEntity student = attempt.getStudent();

            csv.append(escapeCSV(student.getFirstName() + " " + student.getLastName()));
            csv.append(",").append(escapeCSV(student.getEmail()));
            csv.append(",").append(escapeCSV(student.getStudentNumber() != null ? student.getStudentNumber() : ""));
            csv.append(",").append(escapeCSV(student.getStudentGroup() != null ? student.getStudentGroup().getName() : ""));

            // Get answers for this attempt
            Map<Long, AssignmentAnswerEntity> answersByAssignment = answerRepository
                    .findByTestAttemptId(attempt.getId()).stream()
                    .collect(Collectors.toMap(
                            a -> a.getAssignmentEntity().getId(),
                            Function.identity()));

            Map<Long, CodeSubmissionEntity> codeSubmissionsByAssignment = codeSubmissionRepository
                    .findByTestAttemptId(attempt.getId()).stream()
                    .collect(Collectors.toMap(
                            c -> c.getAssignment().getId(),
                            Function.identity()));

            // Score for each assignment
            for (AssignmentEntity assignment : assignments) {
                Float score = null;

                if (assignment.getType() == AssignmentType.CODING) {
                    CodeSubmissionEntity submission = codeSubmissionsByAssignment.get(assignment.getId());
                    if (submission != null) {
                        score = submission.getTotalScore();
                    }
                } else {
                    AssignmentAnswerEntity answer = answersByAssignment.get(assignment.getId());
                    if (answer != null) {
                        score = answer.getScore();
                    }
                }

                csv.append(",").append(score != null ? String.format("%.2f", score) : "N/A");
            }

            // Total score
            Float totalScore = attempt.getScore();
            csv.append(",").append(totalScore != null ? String.format("%.2f", totalScore) : "0.00");
            csv.append(",").append(String.format("%.2f", maxPossible));

            // Percentage
            float percentage = (totalScore != null && maxPossible > 0) ? totalScore / maxPossible * 100f : 0f;
            csv.append(",").append(String.format("%.1f%%", percentage));

            // Status
            int pendingGrading = (int) answerRepository.countUngradedByTestAttemptId(attempt.getId());
            String status = pendingGrading > 0 ? "SUBMITTED" : "GRADED";
            csv.append(",").append(escapeCSV(status));

            // Dates
            csv.append(",").append(attempt.getStartedAt() != null ?
                    escapeCSV(attempt.getStartedAt().format(DATE_FORMATTER)) : "");
            csv.append(",").append(attempt.getFinishedAt() != null ?
                    escapeCSV(attempt.getFinishedAt().format(DATE_FORMATTER)) : "");

            csv.append("\n");
        }

        return csv.toString();
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // If value contains comma, newline, or quote, wrap in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
