package com.edutest.service.userservice;

import com.edutest.api.model.*;
import com.edutest.dto.BatchOperationResult;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.persistance.repository.TestAttemptJpaRepository;
import com.edutest.persistance.repository.TestRepository;
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

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final LoginGeneratorPort loginGenerator;
    private final TestRepository testRepository;
    private final TestAttemptJpaRepository testAttemptJpaRepository;

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

            long testCount = testRepository.countByCreatedBy(user);
            if (testCount > 0) {
                log.warn("Cannot delete teacher {} - authored {} test(s)", userId, testCount);
                throw new IllegalStateException(
                    String.format("Cannot delete teacher - is the author of %d test(s). Deactivate the account instead to preserve test history.", testCount)
                );
            }
        }

        long attemptCount = testAttemptJpaRepository.countByStudentId(userId);
        if (attemptCount > 0) {
            log.warn("Cannot delete user {} - has {} test attempt(s)", userId, attemptCount);
            throw new IllegalStateException(
                String.format("Cannot delete user - has %d test attempt(s). Deactivate the account instead to preserve attempt history.", attemptCount)
            );
        }

        // Soft delete: keep the row (and all history that references it) but hide it from every read.
        // @SQLRestriction on UserEntity excludes deleted_at IS NOT NULL rows from all queries.
        // Users still referenced by NOT-NULL FKs (authored tests, attempts) are blocked above so
        // soft-deletion never leaves a dangling reference that would null-out on load.
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("User {} soft-deleted successfully", userId);
    }

    @Transactional(readOnly = true)
    public List<UserProfile> getDeletedUsers() {
        log.info("Listing soft-deleted users");
        return userRepository.findAllDeleted().stream()
                .map(userMapper::toUserProfile)
                .toList();
    }

    @Transactional
    public UserProfile restoreUser(Long userId) {
        log.info("Restoring user with id: {}", userId);
        UserEntity user = userRepository.findDeletedById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Deleted user not found with id: " + userId));

        userRepository.restoreById(userId);
        log.info("User {} restored successfully", userId);
        return userMapper.toUserProfile(user);
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
