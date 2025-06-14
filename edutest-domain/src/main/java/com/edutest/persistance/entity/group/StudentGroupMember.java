package com.edutest.persistance.entity.group;


import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student_group_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGroupMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @PrePersist
    @PreUpdate
    private void validateStudent() {
        if (student != null && !student.isStudent()) {
            throw new IllegalStateException("Only users with STUDENT role can be group members");
        }
    }
}
