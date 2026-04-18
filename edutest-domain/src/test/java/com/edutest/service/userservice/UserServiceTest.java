package com.edutest.service.userservice;

import com.edutest.domain.user.User;
import com.edutest.domain.user.UserRole;
import com.edutest.persistance.entity.group.StudentGroupEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.persistance.repository.StudentGroupJpaRepository;
import com.edutest.persistance.repository.TestRepository;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.util.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TestRepository testRepository;

    @Mock
    private StudentGroupJpaRepository studentGroupJpaRepository;

    @InjectMocks
    private UserService userService;

    private UserEntity teacherEntity;
    private UserEntity studentEntity;
    private UserEntity adminEntity;
    private User teacherUser;
    private User studentUser;
    private User adminUser;
    private StudentGroupEntity groupEntity;

    @BeforeEach
    void setUp() {
        teacherEntity = new UserEntity();
        teacherEntity.setId(1L);
        teacherEntity.setUsername("teacher1");
        teacherEntity.setEmail("teacher@test.com");
        teacherEntity.setPassword("encodedPassword");
        teacherEntity.setFirstName("John");
        teacherEntity.setLastName("Teacher");
        teacherEntity.setRole(UserEntityRole.TEACHER);
        teacherEntity.setIsActive(true);

        studentEntity = new UserEntity();
        studentEntity.setId(2L);
        studentEntity.setUsername("student1");
        studentEntity.setEmail("student@test.com");
        studentEntity.setPassword("encodedPassword");
        studentEntity.setFirstName("Jane");
        studentEntity.setLastName("Student");
        studentEntity.setRole(UserEntityRole.STUDENT);
        studentEntity.setIsActive(true);

        adminEntity = new UserEntity();
        adminEntity.setId(3L);
        adminEntity.setUsername("admin1");
        adminEntity.setEmail("admin@test.com");
        adminEntity.setPassword("encodedPassword");
        adminEntity.setFirstName("Admin");
        adminEntity.setLastName("User");
        adminEntity.setRole(UserEntityRole.ADMIN);
        adminEntity.setIsActive(true);

        teacherUser = User.builder()
                .username("teacher1")
                .email("teacher@test.com")
                .firstName("John")
                .lastName("Teacher")
                .role(UserRole.TEACHER)
                .isActive(true)
                .build();

        studentUser = User.builder()
                .username("student1")
                .email("student@test.com")
                .firstName("Jane")
                .lastName("Student")
                .role(UserRole.STUDENT)
                .isActive(true)
                .build();

        adminUser = User.builder()
                .username("admin1")
                .email("admin@test.com")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        groupEntity = new StudentGroupEntity();
        groupEntity.setId(10L);
        groupEntity.setName("Group A");
    }

    @Nested
    @DisplayName("validatePassword tests")
    class ValidatePasswordTests {

        @Test
        @DisplayName("Should accept valid password")
        void shouldAcceptValidPassword() {
            // When
            boolean result = userService.validatePassword("Password1!");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject password shorter than 8 characters")
        void shouldRejectShortPassword() {
            // When/Then
            assertThatThrownBy(() -> userService.validatePassword("Pass1!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 8 characters");
        }

        @Test
        @DisplayName("Should reject password without uppercase letter")
        void shouldRejectPasswordWithoutUppercase() {
            // When/Then
            assertThatThrownBy(() -> userService.validatePassword("password1!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("uppercase letter");
        }

        @Test
        @DisplayName("Should reject password without lowercase letter")
        void shouldRejectPasswordWithoutLowercase() {
            // When/Then
            assertThatThrownBy(() -> userService.validatePassword("PASSWORD1!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lowercase letter");
        }

        @Test
        @DisplayName("Should reject password without digit")
        void shouldRejectPasswordWithoutDigit() {
            // When/Then
            assertThatThrownBy(() -> userService.validatePassword("Password!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("digit");
        }

        @Test
        @DisplayName("Should reject password without special character")
        void shouldRejectPasswordWithoutSpecialChar() {
            // When/Then
            assertThatThrownBy(() -> userService.validatePassword("Password1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("special character");
        }
    }

    @Nested
    @DisplayName("lockUserAccount and unlockUserAccount tests")
    class AccountLockingTests {

        @Test
        @DisplayName("Should lock user account")
        void shouldLockUserAccount() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userRepository.save(any(UserEntity.class))).thenReturn(teacherEntity);

            // When
            userService.lockUserAccount(1L, "Suspicious activity");

            // Then
            verify(userRepository).save(argThat(user -> !user.getIsActive()));
        }

        @Test
        @DisplayName("Should do nothing when user not found for locking")
        void shouldDoNothingWhenUserNotFoundForLocking() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            userService.lockUserAccount(999L, "Test");

            // Then
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should unlock user account")
        void shouldUnlockUserAccount() {
            // Given
            teacherEntity.setIsActive(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userRepository.save(any(UserEntity.class))).thenReturn(teacherEntity);

            // When
            userService.unlockUserAccount(1L);

            // Then
            verify(userRepository).save(argThat(UserEntity::getIsActive));
        }
    }

    @Nested
    @DisplayName("canUserCreateTests tests")
    class CanUserCreateTestsTests {

        @Test
        @DisplayName("Should return true for teacher")
        void shouldReturnTrueForTeacher() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));

            // When
            boolean result = userService.canUserCreateTests(1L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true for admin")
        void shouldReturnTrueForAdmin() {
            // Given
            when(userRepository.findById(3L)).thenReturn(Optional.of(adminEntity));

            // When
            boolean result = userService.canUserCreateTests(3L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for student")
        void shouldReturnFalseForStudent() {
            // Given
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));

            // When
            boolean result = userService.canUserCreateTests(2L);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when user not found")
        void shouldReturnFalseWhenUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean result = userService.canUserCreateTests(999L);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("canUserAccessGroup tests")
    class CanUserAccessGroupTests {

        @Test
        @DisplayName("Should return true for admin accessing any group")
        void shouldReturnTrueForAdminAccessingAnyGroup() {
            // Given
            when(userRepository.findById(3L)).thenReturn(Optional.of(adminEntity));

            // When
            boolean result = userService.canUserAccessGroup(3L, 10L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true for student accessing own group")
        void shouldReturnTrueForStudentAccessingOwnGroup() {
            // Given
            studentEntity.setStudentGroup(groupEntity);
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));

            // When
            boolean result = userService.canUserAccessGroup(2L, 10L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for student accessing other group")
        void shouldReturnFalseForStudentAccessingOtherGroup() {
            // Given
            studentEntity.setStudentGroup(groupEntity);
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));

            // When
            boolean result = userService.canUserAccessGroup(2L, 20L);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true for teacher accessing managed group")
        void shouldReturnTrueForTeacherAccessingManagedGroup() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(studentGroupJpaRepository.findByTeacher(teacherEntity)).thenReturn(List.of(groupEntity));

            // When
            boolean result = userService.canUserAccessGroup(1L, 10L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for teacher not managing group")
        void shouldReturnFalseForTeacherNotManagingGroup() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(studentGroupJpaRepository.findByTeacher(teacherEntity)).thenReturn(List.of());

            // When
            boolean result = userService.canUserAccessGroup(1L, 10L);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when user not found")
        void shouldReturnFalseWhenUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean result = userService.canUserAccessGroup(999L, 10L);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("hasUserPermission tests")
    class HasUserPermissionTests {

        @Test
        @DisplayName("Should return true for teacher with CREATE_TEST permission")
        void shouldReturnTrueForTeacherCreateTest() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));

            // When
            boolean result = userService.hasUserPermission(1L, "CREATE_TEST");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true for admin with MANAGE_USERS permission")
        void shouldReturnTrueForAdminManageUsers() {
            // Given
            when(userRepository.findById(3L)).thenReturn(Optional.of(adminEntity));

            // When
            boolean result = userService.hasUserPermission(3L, "MANAGE_USERS");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for teacher with MANAGE_USERS permission")
        void shouldReturnFalseForTeacherManageUsers() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));

            // When
            boolean result = userService.hasUserPermission(1L, "MANAGE_USERS");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true for student with TAKE_TESTS permission")
        void shouldReturnTrueForStudentTakeTests() {
            // Given
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));

            // When
            boolean result = userService.hasUserPermission(2L, "TAKE_TESTS");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for teacher with TAKE_TESTS permission")
        void shouldReturnFalseForTeacherTakeTests() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));

            // When
            boolean result = userService.hasUserPermission(1L, "TAKE_TESTS");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for unknown permission")
        void shouldReturnFalseForUnknownPermission() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));

            // When
            boolean result = userService.hasUserPermission(1L, "UNKNOWN_PERMISSION");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when user not found")
        void shouldReturnFalseWhenUserNotFoundForPermission() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean result = userService.hasUserPermission(999L, "CREATE_TEST");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("changePassword tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
            when(passwordEncoder.encode("NewPassword1!")).thenReturn("newEncodedPassword");
            when(userRepository.save(any(UserEntity.class))).thenReturn(teacherEntity);

            // When
            userService.changePassword(1L, "currentPassword", "NewPassword1!");

            // Then
            verify(userRepository).save(argThat(user -> user.getPassword().equals("newEncodedPassword")));
        }

        @Test
        @DisplayName("Should throw exception when current password is incorrect")
        void shouldThrowExceptionWhenCurrentPasswordIncorrect() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> userService.changePassword(1L, "wrongPassword", "NewPassword1!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Current password is incorrect");
        }

        @Test
        @DisplayName("Should throw exception when new password is invalid")
        void shouldThrowExceptionWhenNewPasswordInvalid() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.changePassword(1L, "currentPassword", "weak"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFoundForPasswordChange() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.changePassword(999L, "current", "NewPassword1!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("updateUserProfile tests")
    class UpdateUserProfileTests {

        @Test
        @DisplayName("Should update user profile successfully")
        void shouldUpdateUserProfileSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userRepository.existsByEmail("newemail@test.com")).thenReturn(false);
            when(userRepository.save(any(UserEntity.class))).thenReturn(teacherEntity);
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);

            // When
            User result = userService.updateUserProfile(1L, "NewFirst", "NewLast", "newemail@test.com");

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Should throw exception when email already in use")
        void shouldThrowExceptionWhenEmailInUse() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.updateUserProfile(1L, null, null, "existing@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email already in use");
        }

        @Test
        @DisplayName("Should not check email uniqueness when email unchanged")
        void shouldNotCheckEmailWhenUnchanged() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userRepository.save(any(UserEntity.class))).thenReturn(teacherEntity);
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);

            // When
            userService.updateUserProfile(1L, "NewFirst", null, "teacher@test.com");

            // Then
            verify(userRepository, never()).existsByEmail(any());
        }
    }

    @Nested
    @DisplayName("canStudentTakeTest tests")
    class CanStudentTakeTestTests {

        @Test
        @DisplayName("Should return false when user is not a student")
        void shouldReturnFalseWhenUserIsNotStudent() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));

            // When
            boolean result = userService.canStudentTakeTest(1L, 100L);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when user not found")
        void shouldReturnFalseWhenUserNotFoundForTakeTest() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean result = userService.canStudentTakeTest(999L, 100L);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when test not found")
        void shouldReturnFalseWhenTestNotFound() {
            // Given
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));
            when(testRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean result = userService.canStudentTakeTest(2L, 999L);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findById, findByUsername, findByEmail tests")
    class FindUserTests {

        @Test
        @DisplayName("Should find user by id")
        void shouldFindUserById() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);

            // When
            User result = userService.findById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("teacher1");
        }

        @Test
        @DisplayName("Should throw exception when user not found by id")
        void shouldThrowExceptionWhenUserNotFoundById() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.findById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should find user by username")
        void shouldFindUserByUsername() {
            // Given
            when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);

            // When
            User result = userService.findByUsername("teacher1");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("teacher1");
        }

        @Test
        @DisplayName("Should find user by email")
        void shouldFindUserByEmail() {
            // Given
            when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);

            // When
            User result = userService.findByEmail("teacher@test.com");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("teacher@test.com");
        }
    }

    @Nested
    @DisplayName("existsByUsername and existsByEmail tests")
    class ExistsTests {

        @Test
        @DisplayName("Should return true when username exists")
        void shouldReturnTrueWhenUsernameExists() {
            // Given
            when(userRepository.existsByUsername("teacher1")).thenReturn(true);

            // When
            boolean result = userService.existsByUsername("teacher1");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when username does not exist")
        void shouldReturnFalseWhenUsernameDoesNotExist() {
            // Given
            when(userRepository.existsByUsername("nonexistent")).thenReturn(false);

            // When
            boolean result = userService.existsByUsername("nonexistent");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            // Given
            when(userRepository.existsByEmail("teacher@test.com")).thenReturn(true);

            // When
            boolean result = userService.existsByEmail("teacher@test.com");

            // Then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("getUserTestCount tests")
    class GetUserTestCountTests {

        @Test
        @DisplayName("Should return test count for user")
        void shouldReturnTestCountForUser() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(testRepository.countByCreatedBy(teacherEntity)).thenReturn(5L);

            // When
            int result = userService.getUserTestCount(1L);

            // Then
            assertThat(result).isEqualTo(5);
        }

        @Test
        @DisplayName("Should return zero when user not found")
        void shouldReturnZeroWhenUserNotFoundForTestCount() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            int result = userService.getUserTestCount(999L);

            // Then
            assertThat(result).isEqualTo(0);
        }
    }
}
