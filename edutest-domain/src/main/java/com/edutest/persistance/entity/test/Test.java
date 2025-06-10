package com.edutest.persistance.entity.test;


import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.group.StudentGroup;
import com.edutest.persistance.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Test extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "time_limit")
    private Integer timeLimit; // w minutach

    @Column(name = "allow_navigation", nullable = false)
    @Builder.Default
    private Boolean allowNavigation = true;

    @Column(name = "randomize_order", nullable = false)
    @Builder.Default
    private Boolean randomizeOrder = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "test_groups",
            joinColumns = @JoinColumn(name = "test_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    @Builder.Default
    private List<StudentGroup> assignedGroups = new ArrayList<>();

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orderNumber ASC")
    @Builder.Default
    private List<Assignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
        return assignments.stream()
                .mapToInt(Assignment::getPoints)
                .sum();
    }

    public int getAssignmentCount() {
        return assignments.size();
    }
}
