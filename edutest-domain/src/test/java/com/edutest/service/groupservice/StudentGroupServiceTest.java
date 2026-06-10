package com.edutest.service.groupservice;

import com.edutest.domain.group.StudentGroup;
import com.edutest.domain.user.User;
import com.edutest.domain.user.UserRole;
import com.edutest.persistance.entity.group.StudentGroupEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.entity.user.UserEntityRole;
import com.edutest.persistance.repository.StudentGroupJpaRepository;
import com.edutest.persistance.repository.StudentGroupRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentGroupServiceTest {

    @Mock
    private StudentGroupRepository studentGroupRepository;

    @Mock
    private StudentGroupJpaRepository studentGroupJpaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private StudentGroupService studentGroupService;

    private UserEntity teacherEntity;
    private UserEntity studentEntity;
    private UserEntity anotherStudentEntity;
    private User teacherUser;
    private User studentUser;
    private StudentGroup studentGroup;
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
        studentEntity.setStudentGroup(null);

        anotherStudentEntity = new UserEntity();
        anotherStudentEntity.setId(3L);
        anotherStudentEntity.setUsername("student2");
        anotherStudentEntity.setEmail("student2@test.com");
        anotherStudentEntity.setFirstName("Bob");
        anotherStudentEntity.setLastName("Student");
        anotherStudentEntity.setRole(UserEntityRole.STUDENT);
        anotherStudentEntity.setIsActive(true);
        anotherStudentEntity.setStudentGroup(null);

        teacherUser = User.builder()
                .username("teacher1")
                .roles(Set.of(UserRole.TEACHER))
                .build();

        studentUser = User.builder()
                .username("student1")
                .roles(Set.of(UserRole.STUDENT))
                .build();

        studentGroup = StudentGroup.builder()
                .name("Group A")
                .description("First group")
                .teachers(new ArrayList<>())
                .students(new ArrayList<>())
                .build();

        groupEntity = new StudentGroupEntity();
        groupEntity.setId(10L);
        groupEntity.setName("Group A");
        groupEntity.setDescription("First group");
    }

    @Nested
    @DisplayName("createStudentGroup tests")
    class CreateStudentGroupTests {

        @Test
        @DisplayName("Should create group successfully without teachers")
        void shouldCreateGroupWithoutTeachers() {
            // Given
            when(studentGroupRepository.existsByName("New Group")).thenReturn(false);
            when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(studentGroup);

            // When
            StudentGroup result = studentGroupService.createStudentGroup("New Group", "Description", null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Group A");
            verify(studentGroupRepository).save(any(StudentGroup.class));
        }

        @Test
        @DisplayName("Should create group with teachers")
        void shouldCreateGroupWithTeachers() {
            // Given
            when(studentGroupRepository.existsByName("New Group")).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);
            when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(studentGroup);

            // When
            StudentGroup result = studentGroupService.createStudentGroup("New Group", "Description", List.of(1L));

            // Then
            assertThat(result).isNotNull();
            verify(studentGroupRepository).save(any(StudentGroup.class));
        }

        @Test
        @DisplayName("Should throw exception when group name already exists")
        void shouldThrowExceptionWhenGroupNameExists() {
            // Given
            when(studentGroupRepository.existsByName("Existing Group")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> studentGroupService.createStudentGroup("Existing Group", "Desc", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should throw exception when teacher not found")
        void shouldThrowExceptionWhenTeacherNotFound() {
            // Given
            when(studentGroupRepository.existsByName("New Group")).thenReturn(false);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> studentGroupService.createStudentGroup("New Group", "Desc", List.of(999L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Teacher not found");
        }

        @Test
        @DisplayName("Should throw exception when user is not a teacher")
        void shouldThrowExceptionWhenUserIsNotTeacher() {
            // Given
            when(studentGroupRepository.existsByName("New Group")).thenReturn(false);
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));
            when(userMapper.toUser(studentEntity)).thenReturn(studentUser);

            // When/Then
            assertThatThrownBy(() -> studentGroupService.createStudentGroup("New Group", "Desc", List.of(2L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("is not a teacher");
        }
    }

    @Nested
    @DisplayName("findById tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find group by id")
        void shouldFindGroupById() {
            // Given
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));

            // When
            StudentGroup result = studentGroupService.findById(10L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Group A");
        }

        @Test
        @DisplayName("Should throw exception when group not found")
        void shouldThrowExceptionWhenGroupNotFound() {
            // Given
            when(studentGroupRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> studentGroupService.findById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Student group not found");
        }
    }

    @Nested
    @DisplayName("updateStudentGroup tests")
    class UpdateStudentGroupTests {

        @Test
        @DisplayName("Should update group name successfully")
        void shouldUpdateGroupName() {
            // Given
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(studentGroupRepository.existsByName("Updated Name")).thenReturn(false);
            when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(studentGroup);

            // When
            StudentGroup result = studentGroupService.updateStudentGroup(10L, "Updated Name", null);

            // Then
            assertThat(result).isNotNull();
            verify(studentGroupRepository).save(any(StudentGroup.class));
        }

        @Test
        @DisplayName("Should throw exception when updating to existing name")
        void shouldThrowExceptionWhenUpdatingToExistingName() {
            // Given
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(studentGroupRepository.existsByName("Existing Name")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> studentGroupService.updateStudentGroup(10L, "Existing Name", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should update description only")
        void shouldUpdateDescriptionOnly() {
            // Given
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(studentGroup);

            // When
            StudentGroup result = studentGroupService.updateStudentGroup(10L, null, "New description");

            // Then
            assertThat(result).isNotNull();
            verify(studentGroupRepository).save(any(StudentGroup.class));
        }
    }

    @Nested
    @DisplayName("deleteStudentGroup tests")
    class DeleteStudentGroupTests {

        @Test
        @DisplayName("Should soft-delete group: set deletedAt, detach students, drop test links")
        void shouldSoftDeleteGroup() {
            // Given a group with one student assigned to it
            studentEntity.setStudentGroup(groupEntity);
            when(studentGroupJpaRepository.findById(10L)).thenReturn(Optional.of(groupEntity));
            when(userRepository.findStudentsByGroupId(10L)).thenReturn(List.of(studentEntity));

            // When
            studentGroupService.deleteStudentGroup(10L);

            // Then — soft delete must NOT hard-delete; it sets deletedAt and saves the group.
            assertThat(groupEntity.getDeletedAt()).isNotNull();
            verify(studentGroupJpaRepository).save(groupEntity);
            verify(studentGroupJpaRepository, never()).deleteById(any());

            // The student is detached so they return to the "no group" pool, but the former group
            // is stamped so restoreGroup can put them back.
            assertThat(studentEntity.getStudentGroup()).isNull();
            assertThat(studentEntity.getDeletedFromGroupId()).isEqualTo(10L);
            verify(userRepository).save(studentEntity);

            // Assignment links to any test are removed.
            verify(studentGroupJpaRepository).removeGroupFromAllTests(10L);
        }

        @Test
        @DisplayName("Should throw exception when group not found")
        void shouldThrowExceptionWhenGroupNotFoundForDelete() {
            // Given
            when(studentGroupJpaRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> studentGroupService.deleteStudentGroup(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Student group not found");
        }
    }

    @Nested
    @DisplayName("restoreGroup tests")
    class RestoreGroupTests {

        @Test
        @DisplayName("Should re-attach a former student who is still group-less and clear the marker")
        void shouldReattachStillGrouplessStudent() {
            // Given — a deleted group whose former student was detached (marker = 10, no current group).
            studentEntity.setStudentGroup(null);
            studentEntity.setDeletedFromGroupId(10L);
            when(studentGroupRepository.findDeletedById(10L)).thenReturn(Optional.of(studentGroup));
            when(studentGroupJpaRepository.findById(10L)).thenReturn(Optional.of(groupEntity));
            when(userRepository.findByDeletedFromGroupId(10L)).thenReturn(List.of(studentEntity));
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));

            // When
            studentGroupService.restoreGroup(10L);

            // Then — group row is restored and the student is put back into it; marker is cleared
            // so a future delete/restore cycle can't double-fire.
            verify(studentGroupRepository).restore(10L);
            assertThat(studentEntity.getStudentGroup()).isSameAs(groupEntity);
            assertThat(studentEntity.getDeletedFromGroupId()).isNull();
            verify(userRepository).save(studentEntity);
        }

        @Test
        @DisplayName("Should NOT move a former student who has since joined another group, but still clear the marker")
        void shouldNotStealStudentFromCurrentGroup() {
            // Given — student was in group 10 (marker = 10) but has since joined another group.
            StudentGroupEntity otherGroup = new StudentGroupEntity();
            otherGroup.setId(20L);
            studentEntity.setStudentGroup(otherGroup);
            studentEntity.setDeletedFromGroupId(10L);
            when(studentGroupRepository.findDeletedById(10L)).thenReturn(Optional.of(studentGroup));
            when(studentGroupJpaRepository.findById(10L)).thenReturn(Optional.of(groupEntity));
            when(userRepository.findByDeletedFromGroupId(10L)).thenReturn(List.of(studentEntity));
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));

            // When
            studentGroupService.restoreGroup(10L);

            // Then — current membership wins; the student stays in the other group. Marker is cleared
            // regardless so it can't re-fire later.
            assertThat(studentEntity.getStudentGroup()).isSameAs(otherGroup);
            assertThat(studentEntity.getDeletedFromGroupId()).isNull();
            verify(userRepository).save(studentEntity);
        }

        @Test
        @DisplayName("Should throw when no deleted group matches the id")
        void shouldThrowWhenDeletedGroupNotFound() {
            // Given
            when(studentGroupRepository.findDeletedById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> studentGroupService.restoreGroup(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Deleted student group not found");
            verify(studentGroupRepository, never()).restore(any());
        }
    }

    @Nested
    @DisplayName("addTeacherToGroup tests")
    class AddTeacherToGroupTests {

        @Test
        @DisplayName("Should add teacher to group successfully")
        void shouldAddTeacherToGroup() {
            // Given
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);
            when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(studentGroup);

            // When
            StudentGroup result = studentGroupService.addTeacherToGroup(10L, 1L);

            // Then
            assertThat(result).isNotNull();
            verify(studentGroupRepository).save(any(StudentGroup.class));
        }

        @Test
        @DisplayName("Should throw exception when user is not a teacher")
        void shouldThrowExceptionWhenUserIsNotTeacher() {
            // Given
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));
            when(userMapper.toUser(studentEntity)).thenReturn(studentUser);

            // When/Then
            assertThatThrownBy(() -> studentGroupService.addTeacherToGroup(10L, 2L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("is not a teacher");
        }

        @Test
        @DisplayName("Should throw exception when teacher already in group")
        void shouldThrowExceptionWhenTeacherAlreadyInGroup() {
            // Given
            studentGroup.getTeachers().add(teacherUser);
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);

            // When/Then
            assertThatThrownBy(() -> studentGroupService.addTeacherToGroup(10L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already assigned");
        }
    }

    @Nested
    @DisplayName("removeTeacherFromGroup tests")
    class RemoveTeacherFromGroupTests {

        @Test
        @DisplayName("Should remove teacher from group successfully")
        void shouldRemoveTeacherFromGroup() {
            // Given
            studentGroup.getTeachers().add(teacherUser);
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);
            when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(studentGroup);

            // When
            StudentGroup result = studentGroupService.removeTeacherFromGroup(10L, 1L);

            // Then
            assertThat(result).isNotNull();
            verify(studentGroupRepository).save(any(StudentGroup.class));
        }

        @Test
        @DisplayName("Should throw exception when teacher not in group")
        void shouldThrowExceptionWhenTeacherNotInGroup() {
            // Given
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));
            when(userMapper.toUser(teacherEntity)).thenReturn(teacherUser);

            // When/Then
            assertThatThrownBy(() -> studentGroupService.removeTeacherFromGroup(10L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not assigned");
        }
    }

    @Nested
    @DisplayName("addStudentToGroup tests")
    class AddStudentToGroupTests {

        @Test
        @DisplayName("Should add student to group successfully")
        void shouldAddStudentToGroup() {
            // Given
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));
            when(studentGroupJpaRepository.findById(10L)).thenReturn(Optional.of(groupEntity));
            when(userRepository.save(any(UserEntity.class))).thenReturn(studentEntity);

            // When
            StudentGroup result = studentGroupService.addStudentToGroup(10L, 2L);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Should throw exception when user is not a student")
        void shouldThrowExceptionWhenUserIsNotStudent() {
            // Given
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacherEntity));

            // When/Then
            assertThatThrownBy(() -> studentGroupService.addStudentToGroup(10L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("is not a student");
        }

        @Test
        @DisplayName("Should throw exception when student already in this group")
        void shouldThrowExceptionWhenStudentAlreadyInThisGroup() {
            // Given
            studentEntity.setStudentGroup(groupEntity);
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));

            // When/Then
            assertThatThrownBy(() -> studentGroupService.addStudentToGroup(10L, 2L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already in this group");
        }

        @Test
        @DisplayName("Should throw exception when student already in another group")
        void shouldThrowExceptionWhenStudentInAnotherGroup() {
            // Given
            StudentGroupEntity anotherGroup = new StudentGroupEntity();
            anotherGroup.setId(20L);
            anotherGroup.setName("Group B");
            studentEntity.setStudentGroup(anotherGroup);

            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));

            // When/Then
            assertThatThrownBy(() -> studentGroupService.addStudentToGroup(10L, 2L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already in group");
        }
    }

    @Nested
    @DisplayName("addStudentsToGroup tests")
    class AddStudentsToGroupTests {

        @Test
        @DisplayName("Should add multiple students to group successfully")
        void shouldAddMultipleStudentsToGroup() {
            // Given
            when(studentGroupJpaRepository.findById(10L)).thenReturn(Optional.of(groupEntity));
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));
            when(userRepository.findById(3L)).thenReturn(Optional.of(anotherStudentEntity));
            when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));

            // When
            StudentGroup result = studentGroupService.addStudentsToGroup(10L, List.of(2L, 3L));

            // Then
            assertThat(result).isNotNull();
            verify(userRepository, times(2)).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Should skip student already in this group")
        void shouldSkipStudentAlreadyInThisGroup() {
            // Given
            studentEntity.setStudentGroup(groupEntity);
            when(studentGroupJpaRepository.findById(10L)).thenReturn(Optional.of(groupEntity));
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));
            when(userRepository.findById(3L)).thenReturn(Optional.of(anotherStudentEntity));
            when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));

            // When
            StudentGroup result = studentGroupService.addStudentsToGroup(10L, List.of(2L, 3L));

            // Then
            assertThat(result).isNotNull();
            verify(userRepository, times(1)).save(any(UserEntity.class));
        }
    }

    @Nested
    @DisplayName("removeStudentFromGroup tests")
    class RemoveStudentFromGroupTests {

        @Test
        @DisplayName("Should remove student from group successfully")
        void shouldRemoveStudentFromGroup() {
            // Given
            studentEntity.setStudentGroup(groupEntity);
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));
            when(userRepository.save(any(UserEntity.class))).thenReturn(studentEntity);
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));

            // When
            StudentGroup result = studentGroupService.removeStudentFromGroup(10L, 2L);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Should throw exception when student not in group")
        void shouldThrowExceptionWhenStudentNotInGroup() {
            // Given
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));

            // When/Then
            assertThatThrownBy(() -> studentGroupService.removeStudentFromGroup(10L, 2L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a member");
        }

        @Test
        @DisplayName("Should throw exception when student in different group")
        void shouldThrowExceptionWhenStudentInDifferentGroup() {
            // Given
            StudentGroupEntity anotherGroup = new StudentGroupEntity();
            anotherGroup.setId(20L);
            anotherGroup.setName("Group B");
            studentEntity.setStudentGroup(anotherGroup);
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));

            // When/Then
            assertThatThrownBy(() -> studentGroupService.removeStudentFromGroup(10L, 2L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a member");
        }
    }

    @Nested
    @DisplayName("getGroupStudents and getGroupTeachers tests")
    class GetGroupMembersTests {

        @Test
        @DisplayName("Should get group students")
        void shouldGetGroupStudents() {
            // Given
            studentGroup.getStudents().add(studentUser);
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));

            // When
            List<User> result = studentGroupService.getGroupStudents(10L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("student1");
        }

        @Test
        @DisplayName("Should get group teachers")
        void shouldGetGroupTeachers() {
            // Given
            studentGroup.getTeachers().add(teacherUser);
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));

            // When
            List<User> result = studentGroupService.getGroupTeachers(10L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("teacher1");
        }

        @Test
        @DisplayName("Should return empty list when no students")
        void shouldReturnEmptyListWhenNoStudents() {
            // Given
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));

            // When
            List<User> result = studentGroupService.getGroupStudents(10L);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("isStudentInGroup tests")
    class IsStudentInGroupTests {

        @Test
        @DisplayName("Should return true when student is in group")
        void shouldReturnTrueWhenStudentInGroup() {
            // Given
            studentGroup.getStudents().add(studentUser);
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));
            when(userMapper.toUser(studentEntity)).thenReturn(studentUser);

            // When
            boolean result = studentGroupService.isStudentInGroup(10L, 2L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when student is not in group")
        void shouldReturnFalseWhenStudentNotInGroup() {
            // Given
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));
            when(userRepository.findById(2L)).thenReturn(Optional.of(studentEntity));
            when(userMapper.toUser(studentEntity)).thenReturn(studentUser);

            // When
            boolean result = studentGroupService.isStudentInGroup(10L, 2L);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getGroupStudentCount tests")
    class GetGroupStudentCountTests {

        @Test
        @DisplayName("Should return correct student count")
        void shouldReturnCorrectStudentCount() {
            // Given
            studentGroup.getStudents().add(studentUser);
            studentGroup.getStudents().add(User.builder().username("student2").build());
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));

            // When
            int result = studentGroupService.getGroupStudentCount(10L);

            // Then
            assertThat(result).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return zero when no students")
        void shouldReturnZeroWhenNoStudents() {
            // Given
            when(studentGroupRepository.findById(10L)).thenReturn(Optional.of(studentGroup));

            // When
            int result = studentGroupService.getGroupStudentCount(10L);

            // Then
            assertThat(result).isEqualTo(0);
        }
    }
}
