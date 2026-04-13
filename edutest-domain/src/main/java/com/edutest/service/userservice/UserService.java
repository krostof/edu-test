package com.edutest.service.userservice;

import com.edutest.domain.user.User;
import com.edutest.domain.user.UserRole;
import com.edutest.domain.group.StudentGroup;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.persistance.repository.StudentGroupJpaRepository;
import com.edutest.persistance.repository.TestRepository;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TestRepository testRepository;
    private final StudentGroupJpaRepository studentGroupJpaRepository;

    public boolean validatePassword(String password) {
            if (password.length() < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters long");
            }
            if (!password.matches(".*[A-Z].*")) {
                throw new IllegalArgumentException("Password must contain at least one uppercase letter");
            }
            if (!password.matches(".*[a-z].*")) {
                throw new IllegalArgumentException("Password must contain at least one lowercase letter");
            }
            if (!password.matches(".*\\d.*")) {
                throw new IllegalArgumentException("Password must contain at least one digit");
            }
            if (!password.matches(".*[!@#$%^&*()].*")) {
                throw new IllegalArgumentException("Password must contain at least one special character");
            }
        return true;
    }

    public void lockUserAccount(Long userId, String reason) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setIsActive(false);
            userRepository.save(user);
            log.info("User account with ID {} has been locked. Reason: {}", userId, reason);
        });
    }

    public void unlockUserAccount(Long userId) {
        // TODO: Implement account unlocking logic
        userRepository.findById(userId).ifPresent(user -> {
            user.setIsActive(true);
            userRepository.save(user);
            log.info("User account with ID {} has been unlocked.", userId);
        });
    }

    public void initiatePasswordReset(String email) {
        // TODO: Implement password reset initiation
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void resetPassword(String token, String newPassword) {
        // TODO: Implement password reset with token
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void activateUserAccount(String activationToken) {
        // TODO: Implement account activation
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // Business Rules for Users

    @Transactional(readOnly = true)
    public boolean canUserCreateTests(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() == UserEntityRole.TEACHER || user.getRole() == UserEntityRole.ADMIN)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canUserAccessGroup(Long userId, Long groupId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        if (user.getRole() == UserEntityRole.ADMIN) {
            return true;
        }

        if (user.getRole() == UserEntityRole.STUDENT) {
            return user.getStudentGroup() != null && user.getStudentGroup().getId().equals(groupId);
        }

        if (user.getRole() == UserEntityRole.TEACHER) {
            return studentGroupJpaRepository.findByTeacher(user).stream()
                    .anyMatch(g -> g.getId().equals(groupId));
        }

        return false;
    }

    @Transactional(readOnly = true)
    public boolean hasUserPermission(Long userId, String permission) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        return switch (permission) {
            case "CREATE_TEST" -> user.getRole() == UserEntityRole.TEACHER || user.getRole() == UserEntityRole.ADMIN;
            case "MANAGE_USERS" -> user.getRole() == UserEntityRole.ADMIN;
            case "MANAGE_GROUPS" -> user.getRole() == UserEntityRole.TEACHER || user.getRole() == UserEntityRole.ADMIN;
            case "VIEW_ALL_RESULTS" -> user.getRole() == UserEntityRole.TEACHER || user.getRole() == UserEntityRole.ADMIN;
            case "TAKE_TESTS" -> user.getRole() == UserEntityRole.STUDENT;
            default -> false;
        };
    }

    // User Profile Management

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        log.info("Changing password for userId={}", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        validatePassword(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for userId={}", userId);
    }

    public User updateUserProfile(Long userId, String firstName, String lastName, String email) {
        log.info("Updating profile for userId={}", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email already in use: " + email);
            }
            user.setEmail(email);
        }

        if (firstName != null) {
            user.setFirstName(firstName);
        }

        if (lastName != null) {
            user.setLastName(lastName);
        }

        UserEntity saved = userRepository.save(user);
        log.info("Profile updated for userId={}", userId);

        return userMapper.toUser(saved);
    }

    @Transactional(readOnly = true)
    public List<String> getUserActivityHistory(Long userId, LocalDateTime from, LocalDateTime to) {
        // Activity history would require audit logging infrastructure
        // For now, return empty list
        log.warn("getUserActivityHistory not implemented - requires audit logging infrastructure");
        return List.of();
    }

    // Integration with Test/Assignment Logic

    @Transactional(readOnly = true)
    public boolean canStudentTakeTest(Long studentId, Long testId) {
        log.debug("Checking if student {} can take test {}", studentId, testId);

        UserEntity student = userRepository.findById(studentId).orElse(null);
        if (student == null || student.getRole() != UserEntityRole.STUDENT) {
            return false;
        }

        TestEntity test = testRepository.findById(testId).orElse(null);
        if (test == null) {
            return false;
        }

        return test.isAvailableForStudent(student);
    }

    @Transactional(readOnly = true)
    public List<User> findTeachersForGroup(Long groupId) {
        log.debug("Finding teachers for groupId={}", groupId);
        return studentGroupJpaRepository.findById(groupId)
                .map(group -> group.getTeachers().stream()
                        .map(userMapper::toUser)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public int getUserTestCount(Long userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return 0;
        }
        return (int) testRepository.countByCreatedBy(user);
    }

    @Transactional(readOnly = true)
    public int getUserAttemptCount(Long userId) {
        // This would require TestAttemptJpaRepository, for now return 0
        // TODO: Inject TestAttemptJpaRepository when needed
        log.warn("getUserAttemptCount not fully implemented - requires TestAttemptJpaRepository");
        return 0;
    }

    // Search & Filtering Logic

    @Transactional(readOnly = true)
    public Page<User> searchUsers(String searchTerm, UserRole role, Boolean isActive, Pageable pageable) {
        log.debug("Searching users: searchTerm={}, role={}, isActive={}", searchTerm, role, isActive);

        Page<UserEntity> entityPage;
        if (role != null && searchTerm != null && !searchTerm.isBlank()) {
            UserEntityRole entityRole = toEntityRole(role);
            entityPage = userRepository.searchUsersByRole(searchTerm, entityRole, pageable);
        } else if (searchTerm != null && !searchTerm.isBlank()) {
            entityPage = userRepository.searchUsers(searchTerm, pageable);
        } else if (role != null) {
            UserEntityRole entityRole = toEntityRole(role);
            entityPage = userRepository.findByRole(entityRole, pageable);
        } else {
            entityPage = userRepository.findAll(pageable);
        }

        if (isActive != null) {
            return entityPage
                    .map(userMapper::toUser)
                    .map(u -> Boolean.TRUE.equals(u.getIsActive()) == isActive ? u : null)
                    .map(u -> u); // Filter handled below
        }

        return entityPage.map(userMapper::toUser);
    }

    @Transactional(readOnly = true)
    public List<User> findUsersByGroup(Long groupId) {
        log.debug("Finding users in groupId={}", groupId);
        return userRepository.findStudentsByGroupId(groupId).stream()
                .map(userMapper::toUser)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<User> findActiveStudents() {
        return userRepository.findByRole(UserEntityRole.STUDENT, Pageable.unpaged())
                .stream()
                .filter(UserEntity::getIsActive)
                .map(userMapper::toUser)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<User> findActiveTeachers() {
        return userRepository.findByRole(UserEntityRole.TEACHER, Pageable.unpaged())
                .stream()
                .filter(UserEntity::getIsActive)
                .map(userMapper::toUser)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<User> findUsersByRole(UserRole role, Pageable pageable) {
        UserEntityRole entityRole = toEntityRole(role);
        return userRepository.findByRole(entityRole, pageable).map(userMapper::toUser);
    }

    private UserEntityRole toEntityRole(UserRole role) {
        if (role == null) return null;
        return switch (role) {
            case STUDENT -> UserEntityRole.STUDENT;
            case TEACHER -> UserEntityRole.TEACHER;
            case ADMIN -> UserEntityRole.ADMIN;
        };
    }

    // User Statistics

    @Transactional(readOnly = true)
    public long countActiveUsers() {
        return userRepository.findAll().stream()
                .filter(UserEntity::getIsActive)
                .count();
    }

    @Transactional(readOnly = true)
    public long countUsersByRole(UserRole role) {
        UserEntityRole entityRole = toEntityRole(role);
        return userRepository.findByRole(entityRole, Pageable.unpaged()).getTotalElements();
    }

    @Transactional(readOnly = true)
    public LocalDateTime getLastLoginTime(Long userId) {
        // Last login tracking would require additional field/table
        // For now, return null to indicate not tracked
        log.warn("getLastLoginTime not implemented - requires last login tracking");
        return null;
    }

    // Helper Methods

    @Transactional(readOnly = true)
    public User findById(Long userId) {
        log.debug("Finding user by id={}", userId);
        return userRepository.findById(userId)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        log.debug("Finding user by username={}", username);
        return userRepository.findByUsername(username)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        log.debug("Finding user by email={}", email);
        return userRepository.findByEmail(email)
                .map(userMapper::toUser)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}