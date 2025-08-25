package com.edutest.domain.test;

import com.edutest.domain.assignment.Assignment;
import com.edutest.domain.group.StudentGroup;
import com.edutest.domain.user.User;
import com.edutest.domain.common.DomainEntity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Test extends DomainEntity {

    private String title;

    private String description;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer timeLimit;

    @Builder.Default
    private Boolean allowNavigation = true;

    @Builder.Default
    private Boolean randomizeOrder = false;

    private User createdBy;

    @Builder.Default
    private List<StudentGroup> assignedGroups = new ArrayList<>();

    @Builder.Default
    private List<Assignment> assignments = new ArrayList<>();

    @Builder.Default
    private List<TestAttempt> attempts = new ArrayList<>();

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startDate) && now.isBefore(endDate);
    }

    public boolean isUpcoming() {
        return LocalDateTime.now().isBefore(startDate);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endDate);
    }

    public boolean isAvailableForStudent(User student) {
        if (!student.isStudent() || !isActive()) {
            return false;
        }

        return assignedGroups.stream()
                .anyMatch(group -> group.containsStudent(student));
    }

    public boolean hasStudentStartedAttempt(User student) {
        return attempts.stream()
                .anyMatch(attempt -> attempt.getStudent().equals(student));
    }

    public TestAttempt getStudentAttempt(User student) {
        return attempts.stream()
                .filter(attempt -> attempt.getStudent().equals(student))
                .findFirst()
                .orElse(null);
    }

    public void addAssignment(Assignment assignment) {
        if (assignment != null) {
            assignment.setTest(this);
            assignment.setOrderNumber(assignments.size() + 1);
            assignments.add(assignment);
        }
    }

    public void addGroup(StudentGroup group) {
        if (group != null && !assignedGroups.contains(group)) {
            assignedGroups.add(group);
        }
    }

    public void removeGroup(StudentGroup group) {
        assignedGroups.remove(group);
    }

    public int getTotalPoints() {
        return (int) assignments.stream()
                .mapToDouble(Assignment::getPoints)
                .sum();
    }

    public int getAssignmentCount() {
        return assignments.size();
    }
}
