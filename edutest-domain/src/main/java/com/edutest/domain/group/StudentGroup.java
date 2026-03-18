package com.edutest.domain.group;

import com.edutest.domain.user.User;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGroup {

    private Long id;

    private String name;

    private String description;

    @Builder.Default
    private List<User> teachers = new ArrayList<>();

    @Builder.Default
    private List<User> students = new ArrayList<>();

    public void addTeacher(User teacher) {
        if (teacher == null || !teacher.isTeacher()) {
            throw new IllegalArgumentException("Only teachers can be added as group teachers");
        }
        if (!teachers.contains(teacher)) {
            teachers.add(teacher);
        }
    }

    public void removeTeacher(User teacher) {
        teachers.remove(teacher);
    }

    public boolean hasTeacher(User teacher) {
        return teachers.contains(teacher);
    }

    public int getTeacherCount() {
        return teachers.size();
    }

    public void addStudent(User student) {
        if (student == null || !student.isStudent()) {
            throw new IllegalArgumentException("Only students can be added to groups");
        }
        if (student.hasGroup() && !student.getStudentGroup().equals(this)) {
            throw new IllegalStateException("Student is already in another group");
        }
        student.setStudentGroup(this);
        students.add(student);
    }

    public void removeStudent(User student) {
        if (students.remove(student)) {
            student.setStudentGroup(null);
        }
    }

    public List<User> getStudents() {
        return students;
    }

    public int getStudentCount() {
        return students.size();
    }

    public boolean containsStudent(User student) {
        return students.contains(student);
    }
}
