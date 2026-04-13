package com.edutest.service.teacher;

import com.edutest.dto.AttemptListItemDto;
import com.edutest.dto.ScoreDistributionItemDto;
import com.edutest.dto.TestStatsSummaryDto;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.AssignmentAnswerJpaRepository;
import com.edutest.persistance.repository.AssignmentJpaRepository;
import com.edutest.persistance.repository.TestAttemptJpaRepository;
import com.edutest.persistance.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherAttemptService {

    private final TestAttemptJpaRepository testAttemptRepository;
    private final TestRepository testRepository;
    private final AssignmentJpaRepository assignmentRepository;
    private final AssignmentAnswerJpaRepository answerRepository;

    @Transactional(readOnly = true)
    public Page<AttemptListItemDto> getAttemptsByTestId(
            Long testId,
            Long groupId,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        TestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found"));

        Float maxPossibleScore = assignmentRepository.sumPointsByTestId(testId);
        if (maxPossibleScore == null) {
            maxPossibleScore = 0f;
        }

        List<TestAttemptEntity> allAttempts = testAttemptRepository.findByTestId(testId);

        // Filter by group if specified
        if (groupId != null) {
            allAttempts = allAttempts.stream()
                    .filter(a -> a.getStudent().getStudentGroup() != null &&
                            a.getStudent().getStudentGroup().getId().equals(groupId))
                    .collect(Collectors.toList());
        }

        // Convert to DTOs with status calculation
        Float finalMaxScore = maxPossibleScore;
        List<AttemptListItemDto> dtos = allAttempts.stream()
                .map(attempt -> mapToListItem(attempt, finalMaxScore))
                .collect(Collectors.toList());

        // Filter by status if specified
        if (status != null && !status.isEmpty()) {
            dtos = dtos.stream()
                    .filter(dto -> dto.getStatus().equals(status))
                    .collect(Collectors.toList());
        }

        // Sort
        Comparator<AttemptListItemDto> comparator = getComparator(sortBy);
        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }
        dtos.sort(comparator);

        // Paginate
        int start = page * size;
        int end = Math.min(start + size, dtos.size());
        List<AttemptListItemDto> pageContent = start < dtos.size()
                ? dtos.subList(start, end)
                : Collections.emptyList();

        return new PageImpl<>(pageContent, PageRequest.of(page, size), dtos.size());
    }

    @Transactional(readOnly = true)
    public TestStatsSummaryDto getTestStatsSummary(Long testId) {
        TestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found"));

        Float maxPossibleScore = assignmentRepository.sumPointsByTestId(testId);
        if (maxPossibleScore == null) {
            maxPossibleScore = 0f;
        }

        List<TestAttemptEntity> allAttempts = testAttemptRepository.findByTestId(testId);

        int totalAttempts = allAttempts.size();
        int completedAttempts = (int) allAttempts.stream().filter(TestAttemptEntity::isFinished).count();
        int inProgressAttempts = totalAttempts - completedAttempts;

        // Get scores for completed attempts
        List<Float> scores = allAttempts.stream()
                .filter(TestAttemptEntity::isFinished)
                .map(TestAttemptEntity::getScore)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        // Calculate graded attempts (no pending manual grading)
        Float finalMaxScore = maxPossibleScore;
        int gradedAttempts = (int) allAttempts.stream()
                .filter(TestAttemptEntity::isFinished)
                .filter(a -> countPendingGrading(a.getId()) == 0)
                .count();

        // Calculate statistics
        Float averageScore = scores.isEmpty() ? null :
                (float) scores.stream().mapToDouble(Float::doubleValue).average().orElse(0);
        Float medianScore = scores.isEmpty() ? null : calculateMedian(scores);
        Float minScore = scores.isEmpty() ? null : scores.get(0);
        Float maxScore = scores.isEmpty() ? null : scores.get(scores.size() - 1);

        Float averageScorePercentage = (averageScore != null && maxPossibleScore > 0)
                ? averageScore / maxPossibleScore * 100f : null;

        // Calculate score distribution
        List<ScoreDistributionItemDto> distribution = calculateScoreDistribution(scores, maxPossibleScore);

        return TestStatsSummaryDto.builder()
                .testId(testId)
                .testTitle(test.getTitle())
                .totalAttempts(totalAttempts)
                .completedAttempts(completedAttempts)
                .inProgressAttempts(inProgressAttempts)
                .gradedAttempts(gradedAttempts)
                .averageScore(averageScore)
                .medianScore(medianScore)
                .minScore(minScore)
                .maxScore(maxScore)
                .averageScorePercentage(averageScorePercentage)
                .scoreDistribution(distribution)
                .build();
    }

    private AttemptListItemDto mapToListItem(TestAttemptEntity attempt, Float maxPossibleScore) {
        UserEntity student = attempt.getStudent();
        int pendingGrading = countPendingGrading(attempt.getId());
        String status = AttemptListItemDto.calculateStatus(attempt.isFinished(), pendingGrading);

        Float scorePercentage = null;
        if (attempt.getScore() != null && maxPossibleScore != null && maxPossibleScore > 0) {
            scorePercentage = attempt.getScore() / maxPossibleScore * 100f;
        }

        return AttemptListItemDto.builder()
                .attemptId(attempt.getId())
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentEmail(student.getEmail())
                .groupId(student.getStudentGroup() != null ? student.getStudentGroup().getId() : null)
                .groupName(student.getStudentGroup() != null ? student.getStudentGroup().getName() : null)
                .startedAt(attempt.getStartedAt())
                .finishedAt(attempt.getFinishedAt())
                .score(attempt.getScore())
                .maxPossibleScore(maxPossibleScore)
                .scorePercentage(scorePercentage)
                .status(status)
                .pendingGradingCount(pendingGrading)
                .build();
    }

    private int countPendingGrading(Long attemptId) {
        return (int) answerRepository.countUngradedByTestAttemptId(attemptId);
    }

    private Comparator<AttemptListItemDto> getComparator(String sortBy) {
        return switch (sortBy != null ? sortBy : "startedAt") {
            case "finishedAt" -> Comparator.comparing(
                    AttemptListItemDto::getFinishedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "score" -> Comparator.comparing(
                    AttemptListItemDto::getScore,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "studentName" -> Comparator.comparing(
                    AttemptListItemDto::getStudentName,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(
                    AttemptListItemDto::getStartedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        };
    }

    private Float calculateMedian(List<Float> sortedScores) {
        if (sortedScores.isEmpty()) {
            return null;
        }
        int size = sortedScores.size();
        if (size % 2 == 0) {
            return (sortedScores.get(size / 2 - 1) + sortedScores.get(size / 2)) / 2f;
        } else {
            return sortedScores.get(size / 2);
        }
    }

    private List<ScoreDistributionItemDto> calculateScoreDistribution(List<Float> scores, Float maxPossibleScore) {
        if (scores.isEmpty() || maxPossibleScore == null || maxPossibleScore <= 0) {
            return Collections.emptyList();
        }

        int[] buckets = new int[10]; // 0-10%, 10-20%, ..., 90-100%
        String[] labels = {"0-10%", "10-20%", "20-30%", "30-40%", "40-50%",
                "50-60%", "60-70%", "70-80%", "80-90%", "90-100%"};

        for (Float score : scores) {
            float percentage = score / maxPossibleScore * 100f;
            int bucket = Math.min((int) (percentage / 10), 9);
            buckets[bucket]++;
        }

        int total = scores.size();
        List<ScoreDistributionItemDto> distribution = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            distribution.add(ScoreDistributionItemDto.of(labels[i], buckets[i], total));
        }

        return distribution;
    }
}
