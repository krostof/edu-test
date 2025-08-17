package com.edutest.persistance.entity.test;


import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.group.StudentGroupEntity;
import com.edutest.persistance.entity.user.UserEntity;
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
public class TestEntity extends BaseEntity {

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
    private UserEntity createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "test_groups",
            joinColumns = @JoinColumn(name = "test_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    @Builder.Default
    private List<StudentGroupEntity> assignedGroups = new ArrayList<>();

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orderNumber ASC")
    @Builder.Default
    private List<AssignmentEntity> assignmentEntities = new ArrayList<>();

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TestAttemptEntity> attempts = new ArrayList<>();

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

    public boolean isAvailableForStudent(UserEntity student) {
        if (!student.isStudent() || !isActive()) {
            return false;
        }

        return assignedGroups.stream()
                .anyMatch(group -> group.containsStudent(student));
    }

    public boolean hasStudentStartedAttempt(UserEntity student) {
        return attempts.stream()
                .anyMatch(attempt -> attempt.getStudent().equals(student));
    }

    public TestAttemptEntity getStudentAttempt(UserEntity student) {
        return attempts.stream()
                .filter(attempt -> attempt.getStudent().equals(student))
                .findFirst()
                .orElse(null);
    }

    public void addAssignment(AssignmentEntity assignmentEntity) {
        if (assignmentEntity != null) {
            assignmentEntity.setTestEntity(this);
            assignmentEntity.setOrderNumber(assignmentEntities.size() + 1);
            assignmentEntities.add(assignmentEntity);
        }
    }

    public void addGroup(StudentGroupEntity group) {
        if (group != null && !assignedGroups.contains(group)) {
            assignedGroups.add(group);
        }
    }

    public void removeGroup(StudentGroupEntity group) {
        assignedGroups.remove(group);
    }

    public int getTotalPoints() {
        return assignmentEntities.stream()
                .mapToInt(AssignmentEntity::getPoints)
                .sum();
    }

    public int getAssignmentCount() {
        return assignmentEntities.size();
    }
}
