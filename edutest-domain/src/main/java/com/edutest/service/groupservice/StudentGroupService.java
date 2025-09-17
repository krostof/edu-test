package com.edutest.service.groupservice;

import com.edutest.domain.group.StudentGroup;
import com.edutest.domain.group.StudentGroupRepository;
import com.edutest.domain.user.User;
import com.edutest.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class StudentGroupService {

    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;

    public StudentGroup createStudentGroup(String name, String description, Long teacherId) {

        log.info("Creating student group: name={}, description={}, teacherId={}", name, description, teacherId);

        User user = userRepository.findById(teacherId).orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isAdmin()) {
            throw new IllegalArgumentException("Only admins can create student groups");
        }

        if (studentGroupRepository.existsByNameAndTeacher(name, user)) {
            throw new IllegalArgumentException("Group with name '" + name + "' already exists for this teacher");
        }

        StudentGroup studentGroup = StudentGroup.builder()
                .name(name)
                .description(description)
                .teacher(user)
                .build();

        StudentGroup savedGroup = studentGroupRepository.save(studentGroup);

        log.info("Group created successfully with id={}", savedGroup.getId());

        return savedGroup;
    }


}
