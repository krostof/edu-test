package com.edutest.persistance.entity.group;

import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "student_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGroupEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private UserEntity teacher;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StudentGroupMemberEntity> members = new ArrayList<>();

    public void addStudent(UserEntity student) {
        if (student == null || !student.isStudent()) {
            throw new IllegalArgumentException("Only students can be added to groups");
        }

        StudentGroupMemberEntity member = StudentGroupMemberEntity.builder()
                .group(this)
                .student(student)
                .build();

        members.add(member);
    }

    public void removeStudent(UserEntity student) {
        members.removeIf(member -> member.getStudent().equals(student));
    }

    public List<UserEntity> getStudents() {
        return members.stream()
                .map(StudentGroupMemberEntity::getStudent)
                .toList();
    }

    public int getStudentCount() {
        return members.size();
    }

    public boolean containsStudent(UserEntity student) {
        return members.stream()
                .anyMatch(member -> member.getStudent().equals(student));
    }
}
