package com.edutest.service.userservice;

import com.edutest.api.model.UserProfile;
import com.edutest.dto.BatchOperationResult;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.persistance.repository.TestAttemptJpaRepository;
import com.edutest.persistance.repository.TestRepository;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.service.port.LoginGeneratorPort;
import com.edutest.util.UserMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private LoginGeneratorPort loginGenerator;
    @Mock
    private TestRepository testRepository;
    @Mock
    private TestAttemptJpaRepository testAttemptJpaRepository;

    @InjectMocks
    private UserManagementService userManagementService;

    private UserEntity student;

    @BeforeEach
    void setUp() {
        student = new UserEntity();
        student.setId(2L);
        student.setUsername("student1");
        student.setEmail("student@test.com");
        student.setFirstName("Jane");
        student.setLastName("Student");
        student.setRole(UserEntityRole.STUDENT);
        student.setIsActive(true);
    }

    @Nested
    @DisplayName("deleteUserWithValidation tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should soft-delete user: set deletedAt and save, never hard-delete")
        void shouldSoftDeleteUser() {
            // Given
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));

            // When
            userManagementService.deleteUserWithValidation(2L);

            // Then — soft delete preserves the row so history (attempts, grades) survives.
            assertThat(student.getDeletedAt()).isNotNull();
            verify(userRepository).save(student);
            verify(userRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should block deleting a teacher that still has groups assigned")
        void shouldBlockTeacherWithGroups() {
            // Given
            UserEntity teacher = new UserEntity();
            teacher.setId(1L);
            teacher.setRole(UserEntityRole.TEACHER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
            when(userRepository.countGroupsByTeacherId(1L)).thenReturn(3L);

            // When/Then — the guard must trip before any state change.
            assertThatThrownBy(() -> userManagementService.deleteUserWithValidation(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("grupy (3)");
            assertThat(teacher.getDeletedAt()).isNull();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should soft-delete a teacher with no groups")
        void shouldSoftDeleteTeacherWithoutGroups() {
            // Given
            UserEntity teacher = new UserEntity();
            teacher.setId(1L);
            teacher.setRole(UserEntityRole.TEACHER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
            when(userRepository.countGroupsByTeacherId(1L)).thenReturn(0L);

            // When
            userManagementService.deleteUserWithValidation(1L);

            // Then
            assertThat(teacher.getDeletedAt()).isNotNull();
            verify(userRepository).save(teacher);
        }

        @Test
        @DisplayName("Should block deleting a student that has test attempts")
        void shouldBlockStudentWithAttempts() {
            // Given — a student referenced by test_attempts.student_id (NOT NULL FK).
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(testAttemptJpaRepository.countByStudentId(2L)).thenReturn(5L);

            // When/Then — soft-deleting would dangle the attempt's student reference, so it is blocked.
            assertThatThrownBy(() -> userManagementService.deleteUserWithValidation(2L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("podejścia do testów (5)");
            assertThat(student.getDeletedAt()).isNull();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should block deleting a teacher that authored tests")
        void shouldBlockTeacherWithAuthoredTests() {
            // Given
            UserEntity teacher = new UserEntity();
            teacher.setId(1L);
            teacher.setRole(UserEntityRole.TEACHER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
            when(userRepository.countGroupsByTeacherId(1L)).thenReturn(0L);
            when(testRepository.countByCreatedBy(teacher)).thenReturn(2L);

            // When/Then
            assertThatThrownBy(() -> userManagementService.deleteUserWithValidation(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("autorem testów (2)");
            assertThat(teacher.getDeletedAt()).isNull();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userManagementService.deleteUserWithValidation(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("restoreUser tests")
    class RestoreUserTests {

        @Test
        @DisplayName("Should restore a soft-deleted user via the deleted-only lookup")
        void shouldRestoreDeletedUser() {
            // Given — restore must resolve through findDeletedById; @SQLRestriction hides the row
            // from the ordinary findById, so a plain lookup would 404 a deleted user.
            UserProfile profile = new UserProfile();
            when(userRepository.findDeletedById(2L)).thenReturn(Optional.of(student));
            when(userMapper.toUserProfile(student)).thenReturn(profile);

            // When
            UserProfile result = userManagementService.restoreUser(2L);

            // Then — the dedicated UPDATE clears deleted_at; restore must not go through save()
            // because the entity is filtered out of managed reads.
            verify(userRepository).restoreById(2L);
            assertThat(result).isSameAs(profile);
        }

        @Test
        @DisplayName("Should throw when no deleted user matches the id")
        void shouldThrowWhenDeletedUserNotFound() {
            // Given — id is unknown OR the user is still active (not deleted); both must 404.
            when(userRepository.findDeletedById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userManagementService.restoreUser(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Deleted user not found");
            verify(userRepository, never()).restoreById(any());
        }
    }

    @Nested
    @DisplayName("getDeletedUsers tests")
    class GetDeletedUsersTests {

        @Test
        @DisplayName("Should list only soft-deleted users mapped to profiles")
        void shouldListDeletedUsers() {
            // Given — the deleted-only query is the single source; an empty active table must not
            // leak into this list, hence the dedicated findAllDeleted rather than findAll.
            UserProfile profile = new UserProfile();
            when(userRepository.findAllDeleted()).thenReturn(List.of(student));
            when(userMapper.toUserProfile(student)).thenReturn(profile);

            // When
            List<UserProfile> result = userManagementService.getDeletedUsers();

            // Then
            assertThat(result).containsExactly(profile);
        }
    }

    @Nested
    @DisplayName("batchDeleteUsers tests")
    class BatchDeleteUsersTests {

        @Test
        @DisplayName("Should report per-user outcomes and keep going past a blocked deletion")
        void shouldContinueAfterBlockedDeletion() {
            // Given — student (id 2) is deletable; teacher (id 1) still owns groups and must be blocked.
            // The point of the batch wrapper is that one failure does not abort the rest.
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(testAttemptJpaRepository.countByStudentId(2L)).thenReturn(0L);

            UserEntity teacher = new UserEntity();
            teacher.setId(1L);
            teacher.setRole(UserEntityRole.TEACHER);
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
            when(userRepository.countGroupsByTeacherId(1L)).thenReturn(2L);

            // When
            BatchOperationResult result = userManagementService.batchDeleteUsers(List.of(2L, 1L));

            // Then — the deletable user is soft-deleted and saved; the blocked one is recorded as
            // an error without mutating its state.
            assertThat(result.getSuccessCount()).isEqualTo(1);
            assertThat(result.getFailedCount()).isEqualTo(1);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("User ID 1").contains("grupy");
            assertThat(student.getDeletedAt()).isNotNull();
            assertThat(teacher.getDeletedAt()).isNull();
            verify(userRepository).save(student);
            verify(userRepository, never()).save(teacher);
        }
    }
}
