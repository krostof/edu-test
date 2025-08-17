package com.edutest.domain.group;

import com.edutest.domain.user.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGroupMember {
    private StudentGroup group;
    private User student;
}
