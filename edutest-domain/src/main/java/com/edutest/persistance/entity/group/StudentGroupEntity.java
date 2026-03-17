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

    @OneToMany(mappedBy = "studentGroup", fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserEntity> students = new ArrayList<>();

    public void addStudent(UserEntity student) {
        if (student == null || !student.isStudent()) {
            throw new IllegalArgumentException("Only students can be added to groups");
        }
        if (student.getStudentGroup() != null && !student.getStudentGroup().equals(this)) {
            throw new IllegalStateException("Student is already in another group: " +
                student.getStudentGroup().getName());
        }
        student.setStudentGroup(this);
        students.add(student);
    }

    public void removeStudent(UserEntity student) {
        if (students.remove(student)) {
            student.setStudentGroup(null);
        }
    }

    public List<UserEntity> getStudents() {
        return students;
    }

    public int getStudentCount() {
        return students.size();
    }

    public boolean containsStudent(UserEntity student) {
        return students.contains(student);
    }
}
