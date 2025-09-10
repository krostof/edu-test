package com.edutest.service.userservice;

import com.edutest.api.model.*;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.util.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserProfile createStudent(CreateStudentRequest request) {
        validateUserDoesNotExist(request.getUsername(), request.getEmail());
        
        UserEntity userEntity = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .studentNumber(request.getStudentNumber())
                .role(UserEntityRole.STUDENT)
                .isActive(true)
                .build();
        
        UserEntity savedUser = userRepository.save(userEntity);
        return userMapper.toUserProfile(savedUser);
    }

    @Transactional
    public UserProfile createTeacher(CreateTeacherRequest request) {
        validateUserDoesNotExist(request.getUsername(), request.getEmail());
        
        UserEntity userEntity = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserEntityRole.TEACHER)
                .isActive(true)
                .build();
        
        UserEntity savedUser = userRepository.save(userEntity);
        return userMapper.toUserProfile(savedUser);
    }

    public UserPageResponse getAllUsers(UserRole role, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> userPage;
        
        if (role != null) {
            UserEntityRole entityRole = mapToEntityRole(role);
            userPage = userRepository.findByRole(entityRole, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }
        
        UserPageResponse response = new UserPageResponse();
        response.setContent(userPage.getContent().stream()
                .map(userMapper::toUserProfile)
                .toList());
        response.setTotalElements(userPage.getTotalElements());
        response.setTotalPages(userPage.getTotalPages());
        response.setNumber(userPage.getNumber());
        response.setSize(userPage.getSize());
        response.setNumberOfElements(userPage.getNumberOfElements());
        response.setFirst(userPage.isFirst());
        response.setLast(userPage.isLast());
        
        return response;
    }

    public UserProfile getUserById(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return userMapper.toUserProfile(user);
    }

    @Transactional
    public UserProfile updateUser(Long userId, UpdateUserRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException("Username already exists: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }
        
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        
        UserEntity savedUser = userRepository.save(user);
        return userMapper.toUserProfile(savedUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }

    private void validateUserDoesNotExist(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
    }

    private UserEntityRole mapToEntityRole(UserRole role) {
        return switch (role) {
            case STUDENT -> UserEntityRole.STUDENT;
            case TEACHER -> UserEntityRole.TEACHER;
            case ADMIN -> UserEntityRole.ADMIN;
        };
    }
}
