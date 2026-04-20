package com.edutest.service.testservice;

import com.edutest.domain.test.Test;
import com.edutest.domain.group.StudentGroup;
import com.edutest.persistance.entity.group.StudentGroupEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.persistance.repository.StudentGroupJpaRepository;
import com.edutest.persistance.repository.TestRepository;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.util.TestMapper;
import com.edutest.util.UserMapper;
import com.edutest.domain.user.User;
import com.edutest.domain.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentGroupJpaRepository studentGroupJpaRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private TestMapper testMapper;

    @InjectMocks
    private TestService testService;

    private UserEntity teacherEntity;
    private UserEntity studentEntity;
    private UserEntity adminEntity;
    private User teacherUser;
    private User studentUser;
    private TestEntity testEntity;
    private Test testDomain;
    private StudentGroupEntity groupEntity;

    @BeforeEach
    void setUp() {
        teacherEntity = new UserEntity();
        teacherEntity.setId(1L);
        teacherEntity.setUsername("teacher1");
        teacherEntity.setEmail("teacher@test.com");
        teacherEntity.setFirstName("John");
        teacherEntity.setLastName("Teacher");
        teacherEntity.setRole(UserEntityRole.TEACHER);
        teacherEntity.setIsActive(true);

        studentEntity = new UserEntity();
        studentEntity.setId(2L);
        studentEntity.setUsername("student1");
        studentEntity.setEmail("student@test.com");
        studentEntity.setFirstName("Jane");
        studentEntity.setLastName("Student");
        studentEntity.setRole(UserEntityRole.STUDENT);
        studentEntity.setIsActive(true);

        adminEntity = new UserEntity();
        adminEntity.setId(3L);
        adminEntity.setUsername("admin1");
        adminEntity.setEmail("admin@test.com");
        adminEntity.setFirstName("Admin");
        adminEntity.setLastName("User");
        adminEntity.setRole(UserEntityRole.ADMIN);
        adminEntity.setIsActive(true);

        teacherUser = User.builder()
                .username("teacher1")
                .roles(Set.of(UserRole.TEACHER))
                .build();

        studentUser = User.builder()
                .username("student1")
                .roles(Set.of(UserRole.STUDENT))
                .build();

        testEntity = new TestEntity();
        testEntity.setId(100L);
        testEntity.setTitle("Math Test");
        testEntity.setDescription("Basic math test");
        testEntity.setStartDate(LocalDateTime.now().plusDays(1));
        testEntity.setEndDate(LocalDateTime.now().plusDays(7));
        testEntity.setTimeLimit(60);
        testEntity.setAllowNavigation(true);
        testEntity.setRandomizeOrder(false);
        testEntity.setCreatedBy(teacherEntity);
        testEntity.setAssignedGroups(new ArrayList<>());
        testEntity.setAttempts(new ArrayList<>());

        testDomain = Test.builder()
                .title("Math Test")
                .description("Basic math test")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(7))
                .timeLimit(60)
                .allowNavigation(true)
                .randomizeOrder(false)
                .build();

        groupEntity = new StudentGroupEntity();
        groupEntity.setId(10L);
        groupEntity.setName("Group A");
        groupEntity.setDescription("First group");
    }

    @Nested
    @DisplayName("createTest tests")
    class CreateTestTests {

        @org.junit.jupiter.api.Test
        @DisplayName("Should create test successfully when user is teacher")
        void shouldCreateTestWhenUserIsTeacher() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().plusDays(1);
            LocalDateTime endDate = LocalDateTime.now().plusDays(7);

            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);
            when(testRepository.existsByTitleAndCreatedBy("New Test", teacherEntity)).thenReturn(false);
            when(testRepository.save(any(TestEntity.class))).thenReturn(testEntity);
            when(testMapper.toDomain(testEntity)).thenReturn(testDomain);

            // When
            Test result = testService.createTest("New Test", "Description", startDate, endDate,
                    60, true, false, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Math Test");
            verify(testRepository).save(any(TestEntity.class));
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when creator not found")
        void shouldThrowExceptionWhenCreatorNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> testService.createTest("Test", "Desc",
                    LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(7),
                    60, true, false, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Creator not found");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when user is student")
        void shouldThrowExceptionWhenUserIsStudent() {
            // Given
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));
            when(userMapper.toUser(studentEntity)).thenReturn(studentUser);

            // When/Then
            assertThatThrownBy(() -> testService.createTest("Test", "Desc",
                    LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(7),
                    60, true, false, 2L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only teachers can create tests");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when title already exists for user")
        void shouldThrowExceptionWhenTitleAlreadyExists() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);
            when(testRepository.existsByTitleAndCreatedBy("Existing Test", teacherEntity)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> testService.createTest("Existing Test", "Desc",
                    LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(7),
                    60, true, false, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when start date is after end date")
        void shouldThrowExceptionWhenStartDateAfterEndDate() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);

            LocalDateTime startDate = LocalDateTime.now().plusDays(10);
            LocalDateTime endDate = LocalDateTime.now().plusDays(5);

            // When/Then
            assertThatThrownBy(() -> testService.createTest("Test", "Desc",
                    startDate, endDate, 60, true, false, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date cannot be after end date");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when end date is in the past")
        void shouldThrowExceptionWhenEndDateInPast() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);

            LocalDateTime startDate = LocalDateTime.now().minusDays(10);
            LocalDateTime endDate = LocalDateTime.now().minusDays(5);

            // When/Then
            assertThatThrownBy(() -> testService.createTest("Test", "Desc",
                    startDate, endDate, 60, true, false, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date cannot be in the past");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when dates are null")
        void shouldThrowExceptionWhenDatesAreNull() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);

            // When/Then
            assertThatThrownBy(() -> testService.createTest("Test", "Desc",
                    null, LocalDateTime.now().plusDays(7), 60, true, false, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }
    }

    @Nested
    @DisplayName("findById tests")
    class FindByIdTests {

        @org.junit.jupiter.api.Test
        @DisplayName("Should find test by id")
        void shouldFindTestById() {
            // Given
            when(testRepository.findById(100L)).thenReturn(Optional.of(testEntity));
            when(testMapper.toDomain(testEntity)).thenReturn(testDomain);

            // When
            Test result = testService.findById(100L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Math Test");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when test not found")
        void shouldThrowExceptionWhenTestNotFound() {
            // Given
            when(testRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> testService.findById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Test not found");
        }
    }

    @Nested
    @DisplayName("updateTest tests")
    class UpdateTestTests {

        @org.junit.jupiter.api.Test
        @DisplayName("Should update test title successfully")
        void shouldUpdateTestTitle() {
            // Given
            when(testRepository.findById(100L)).thenReturn(Optional.of(testEntity));
            when(testRepository.existsByTitleAndCreatedBy("Updated Title", teacherEntity)).thenReturn(false);
            when(testRepository.save(any(TestEntity.class))).thenReturn(testEntity);
            when(testMapper.toDomain(testEntity)).thenReturn(testDomain);

            // When
            Test result = testService.updateTest(100L, "Updated Title", null, null, null, null, null, null);

            // Then
            assertThat(result).isNotNull();
            verify(testRepository).save(any(TestEntity.class));
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when updating to existing title")
        void shouldThrowExceptionWhenUpdatingToExistingTitle() {
            // Given
            when(testRepository.findById(100L)).thenReturn(Optional.of(testEntity));
            when(testRepository.existsByTitleAndCreatedBy("Existing Title", teacherEntity)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> testService.updateTest(100L, "Existing Title", null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should update test dates with validation")
        void shouldUpdateTestDatesWithValidation() {
            // Given
            LocalDateTime newStartDate = LocalDateTime.now().plusDays(2);
            LocalDateTime newEndDate = LocalDateTime.now().plusDays(10);

            when(testRepository.findById(100L)).thenReturn(Optional.of(testEntity));
            when(testRepository.save(any(TestEntity.class))).thenReturn(testEntity);
            when(testMapper.toDomain(testEntity)).thenReturn(testDomain);

            // When
            Test result = testService.updateTest(100L, null, null, newStartDate, newEndDate, null, null, null);

            // Then
            assertThat(result).isNotNull();
            verify(testRepository).save(any(TestEntity.class));
        }
    }

    @Nested
    @DisplayName("deleteTest tests")
    class DeleteTestTests {

        @org.junit.jupiter.api.Test
        @DisplayName("Should delete test when no attempts exist")
        void shouldDeleteTestWhenNoAttempts() {
            // Given
            testEntity.setAttempts(new ArrayList<>());
            when(testRepository.findById(100L)).thenReturn(Optional.of(testEntity));

            // When
            testService.deleteTest(100L);

            // Then
            verify(testRepository).deleteById(100L);
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when test has attempts")
        void shouldThrowExceptionWhenTestHasAttempts() {
            // Given
            List<TestAttemptEntity> attempts = new ArrayList<>();
            attempts.add(new TestAttemptEntity());
            testEntity.setAttempts(attempts);
            when(testRepository.findById(100L)).thenReturn(Optional.of(testEntity));

            // When/Then
            assertThatThrownBy(() -> testService.deleteTest(100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete test with existing attempts");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when test not found")
        void shouldThrowExceptionWhenTestNotFoundForDelete() {
            // Given
            when(testRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> testService.deleteTest(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Test not found");
        }
    }

    @Nested
    @DisplayName("assignGroupToTest tests")
    class AssignGroupToTestTests {

        @org.junit.jupiter.api.Test
        @DisplayName("Should assign group to test successfully")
        void shouldAssignGroupToTest() {
            // Given
            when(testRepository.findById(100L)).thenReturn(Optional.of(testEntity));
            when(studentGroupJpaRepository.findById(10L)).thenReturn(Optional.of(groupEntity));
            when(testRepository.save(any(TestEntity.class))).thenReturn(testEntity);
            when(testMapper.toDomain(testEntity)).thenReturn(testDomain);

            // When
            Test result = testService.assignGroupToTest(100L, 10L);

            // Then
            assertThat(result).isNotNull();
            verify(testRepository).save(any(TestEntity.class));
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when group already assigned")
        void shouldThrowExceptionWhenGroupAlreadyAssigned() {
            // Given
            testEntity.getAssignedGroups().add(groupEntity);
            when(testRepository.findById(100L)).thenReturn(Optional.of(testEntity));
            when(studentGroupJpaRepository.findById(10L)).thenReturn(Optional.of(groupEntity));

            // When/Then
            assertThatThrownBy(() -> testService.assignGroupToTest(100L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Group is already assigned");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when test not found")
        void shouldThrowExceptionWhenTestNotFoundForAssign() {
            // Given
            when(testRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> testService.assignGroupToTest(999L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Test not found");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when group not found")
        void shouldThrowExceptionWhenGroupNotFound() {
            // Given
            when(testRepository.findById(100L)).thenReturn(Optional.of(testEntity));
            when(studentGroupJpaRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> testService.assignGroupToTest(100L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Group not found");
        }
    }

    @Nested
    @DisplayName("removeGroupFromTest tests")
    class RemoveGroupFromTestTests {

        @org.junit.jupiter.api.Test
        @DisplayName("Should remove group from test successfully")
        void shouldRemoveGroupFromTest() {
            // Given
            testEntity.getAssignedGroups().add(groupEntity);
            when(testRepository.findById(100L)).thenReturn(Optional.of(testEntity));
            when(studentGroupJpaRepository.findById(10L)).thenReturn(Optional.of(groupEntity));
            when(testRepository.save(any(TestEntity.class))).thenReturn(testEntity);
            when(testMapper.toDomain(testEntity)).thenReturn(testDomain);

            // When
            Test result = testService.removeGroupFromTest(100L, 10L);

            // Then
            assertThat(result).isNotNull();
            verify(testRepository).save(any(TestEntity.class));
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when group not assigned to test")
        void shouldThrowExceptionWhenGroupNotAssigned() {
            // Given
            when(testRepository.findById(100L)).thenReturn(Optional.of(testEntity));
            when(studentGroupJpaRepository.findById(10L)).thenReturn(Optional.of(groupEntity));

            // When/Then
            assertThatThrownBy(() -> testService.removeGroupFromTest(100L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Group is not assigned");
        }
    }

    @Nested
    @DisplayName("findAvailableTestsForStudent tests")
    class FindAvailableTestsForStudentTests {

        @org.junit.jupiter.api.Test
        @DisplayName("Should find available tests for student")
        void shouldFindAvailableTestsForStudent() {
            // Given
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));
            when(userMapper.toUser(studentEntity)).thenReturn(studentUser);
            when(testRepository.findAvailableTestsForStudent(2L)).thenReturn(List.of(testEntity));
            when(testMapper.toDomain(testEntity)).thenReturn(testDomain);

            // When
            List<Test> result = testService.findAvailableTestsForStudent(2L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Math Test");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when user is not a student")
        void shouldThrowExceptionWhenUserIsNotStudent() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);

            // When/Then
            assertThatThrownBy(() -> testService.findAvailableTestsForStudent(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("is not a student");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when student not found")
        void shouldThrowExceptionWhenStudentNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> testService.findAvailableTestsForStudent(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Student not found");
        }
    }

    @Nested
    @DisplayName("getTestGroups tests")
    class GetTestGroupsTests {

        @org.junit.jupiter.api.Test
        @DisplayName("Should get groups for test")
        void shouldGetGroupsForTest() {
            // Given
            testEntity.getAssignedGroups().add(groupEntity);
            when(testRepository.findById(100L)).thenReturn(Optional.of(testEntity));

            // When
            List<StudentGroup> result = testService.getTestGroups(100L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Group A");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should return empty list when no groups assigned")
        void shouldReturnEmptyListWhenNoGroups() {
            // Given
            when(testRepository.findById(100L)).thenReturn(Optional.of(testEntity));

            // When
            List<StudentGroup> result = testService.getTestGroups(100L);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findTestsByGroup tests")
    class FindTestsByGroupTests {

        @org.junit.jupiter.api.Test
        @DisplayName("Should find tests by group")
        void shouldFindTestsByGroup() {
            // Given
            when(studentGroupJpaRepository.existsById(10L)).thenReturn(true);
            when(testRepository.findTestsByGroupId(10L)).thenReturn(List.of(testEntity));
            when(testMapper.toDomain(testEntity)).thenReturn(testDomain);

            // When
            List<Test> result = testService.findTestsByGroup(10L);

            // Then
            assertThat(result).hasSize(1);
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Should throw exception when group not found")
        void shouldThrowExceptionWhenGroupNotFoundForFindTests() {
            // Given
            when(studentGroupJpaRepository.existsById(999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> testService.findTestsByGroup(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Group not found");
        }
    }
}
