package com.edutest.service.userservice;

import com.edutest.api.model.CreateStudentRequest;
import com.edutest.api.model.UserProfile;
import com.edutest.persistance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;

     public UserProfile createStudent(CreateStudentRequest request) {

         UserProfile save = userRepository.save();

         return new UserProfile();
     }


}
