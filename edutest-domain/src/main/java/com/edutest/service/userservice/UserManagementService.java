package com.edutest.service.userservice;

import com.edutest.api.model.*;
import com.edutest.dto.BatchOperationResult;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.service.port.LoginGeneratorPort;
import com.edutest.util.UserMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final LoginGeneratorPort loginGenerator;

    @Transactional
    public UserProfile createStudent(CreateStudentRequest request) {
        validateUserDoesNotExist(request.getEmail());
        
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        UserEntity userEntity = userMapper.toStudentEntity(request, encodedPassword);

        userEntity.setUsername(loginGenerator.generateLogin(request.getFirstName(), request.getLastName()));

        UserEntity savedUser = userRepository.save(userEntity);
        return userMapper.toUserProfile(savedUser);
    }

    @Transactional
    public UserProfile createTeacher(CreateTeacherRequest request) {
        validateUserDoesNotExist(request.getEmail());
        
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        UserEntity userEntity = userMapper.toTeacherEntity(request, encodedPassword);

        UserEntity savedUser = userRepository.save(userEntity);
        return userMapper.toUserProfile(savedUser);
    }

    public UserPageResponse getAllUsers(UserRole role, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> userPage;

        if (search != null && !search.trim().isEmpty()) {
            if (role != null) {
                UserEntityRole entityRole = userMapper.toEntityRole(role);
                userPage = userRepository.searchUsersByRole(search.trim(), entityRole, pageable);
            } else {
                userPage = userRepository.searchUsers(search.trim(), pageable);
            }
        } else {
            if (role != null) {
                UserEntityRole entityRole = userMapper.toEntityRole(role);
                userPage = userRepository.findByRole(entityRole, pageable);
            } else {
                userPage = userRepository.findAll(pageable);
            }
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
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        return userMapper.toUserProfile(user);
    }

    @Transactional
    public UserProfile updateUser(Long userId, UpdateUserRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

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
    public UserProfile activateUser(Long userId) {
        log.info("Activating user with id: {}", userId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        user.setIsActive(true);
        UserEntity savedUser = userRepository.save(user);
        log.info("User {} activated successfully", userId);
        return userMapper.toUserProfile(savedUser);
    }

    @Transactional
    public UserProfile deactivateUser(Long userId) {
        log.info("Deactivating user with id: {}", userId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        user.setIsActive(false);
        UserEntity savedUser = userRepository.save(user);
        log.info("User {} deactivated successfully", userId);
        return userMapper.toUserProfile(savedUser);
    }

    @Transactional
    public void deleteUserWithValidation(Long userId) {
        log.info("Attempting to delete user with id: {}", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getRole() == UserEntityRole.TEACHER) {
            long groupCount = userRepository.countGroupsByTeacherId(userId);
            if (groupCount > 0) {
                log.warn("Cannot delete teacher {} - has {} groups assigned", userId, groupCount);
                throw new IllegalStateException(
                    String.format("Cannot delete teacher - has %d group(s) assigned. Please reassign or delete groups first.", groupCount)
                );
            }
        }

        userRepository.deleteById(userId);
        log.info("User {} deleted successfully", userId);
    }

    @Transactional
    public BatchOperationResult batchDeactivateUsers(List<Long> userIds) {
        log.info("Starting batch deactivation for {} users", userIds.size());
        BatchOperationResult result = BatchOperationResult.builder().build();

        for (Long userId : userIds) {
            try {
                deactivateUser(userId);
                result.incrementSuccess();
            } catch (Exception e) {
                log.error("Failed to deactivate user {}: {}", userId, e.getMessage());
                result.addError(userId, e.getMessage());
            }
        }

        log.info("Batch deactivation completed: {} succeeded, {} failed",
                 result.getSuccessCount(), result.getFailedCount());
        return result;
    }

    @Transactional
    public BatchOperationResult batchDeleteUsers(List<Long> userIds) {
        log.info("Starting batch deletion for {} users", userIds.size());
        BatchOperationResult result = BatchOperationResult.builder().build();

        for (Long userId : userIds) {
            try {
                deleteUserWithValidation(userId);
                result.incrementSuccess();
            } catch (Exception e) {
                log.error("Failed to delete user {}: {}", userId, e.getMessage());
                result.addError(userId, e.getMessage());
            }
        }

        log.info("Batch deletion completed: {} succeeded, {} failed",
                 result.getSuccessCount(), result.getFailedCount());
        return result;
    }

    private void validateUserDoesNotExist(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
    }
}
