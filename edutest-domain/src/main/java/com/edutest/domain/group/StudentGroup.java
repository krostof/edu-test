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

    private User teacher;

    @Builder.Default
    private List<User> students = new ArrayList<>();

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
