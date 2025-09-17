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
    private List<StudentGroupMember> members = new ArrayList<>();

    public void addStudent(User student) {
        if (student == null || !student.isStudent()) {
            throw new IllegalArgumentException("Only students can be added to groups");
        }

        StudentGroupMember member = StudentGroupMember.builder()
                .group(this)
                .student(student)
                .build();

        members.add(member);
    }

    public void removeStudent(User student) {
        members.removeIf(member -> member.getStudent().equals(student));
    }

    public List<User> getStudents() {
        return members.stream()
                .map(StudentGroupMember::getStudent)
                .toList();
    }

    public int getStudentCount() {
        return members.size();
    }

    public boolean containsStudent(User student) {
        return members.stream()
                .anyMatch(member -> member.getStudent().equals(student));
    }
}
