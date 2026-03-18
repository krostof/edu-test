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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "group_teachers",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "teacher_id")
    )
    @Builder.Default
    private List<UserEntity> teachers = new ArrayList<>();

    @OneToMany(mappedBy = "studentGroup", fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserEntity> students = new ArrayList<>();

    public void addTeacher(UserEntity teacher) {
        if (teacher == null || !teacher.isTeacher()) {
            throw new IllegalArgumentException("Only teachers can be added as group teachers");
        }
        if (!teachers.contains(teacher)) {
            teachers.add(teacher);
        }
    }

    public void removeTeacher(UserEntity teacher) {
        teachers.remove(teacher);
    }

    public boolean hasTeacher(UserEntity teacher) {
        return teachers.contains(teacher);
    }

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

    public int getTeacherCount() {
        return teachers.size();
    }
}
