package com.edutest.service.attachment;

import com.edutest.persistance.entity.attachment.AttachmentEntity;
import com.edutest.persistance.entity.assigment.openquestion.OpenQuestionAssignmentEntityEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.AssignmentJpaRepository;
import com.edutest.persistance.repository.AttachmentRepository;
import com.edutest.persistance.repository.TestAttemptJpaRepository;
import com.edutest.persistance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private TestAttemptJpaRepository testAttemptRepository;

    @Mock
    private AssignmentJpaRepository assignmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AttachmentService attachmentService;

    @TempDir
    Path tempDir;

    private TestEntity testEntity;
    private TestAttemptEntity attemptEntity;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(attachmentService, "storagePath", tempDir.toString());
        ReflectionTestUtils.setField(attachmentService, "maxFileSize", 10485760L);
        ReflectionTestUtils.setField(attachmentService, "maxAttachmentsPerAnswer", 5);

        testEntity = new TestEntity();
        testEntity.setId(1L);

        userEntity = new UserEntity();
        userEntity.setId(100L);
        userEntity.setUsername("student1");

        attemptEntity = new TestAttemptEntity();
        attemptEntity.setId(10L);
        attemptEntity.setTestEntity(testEntity);
        attemptEntity.setStudent(userEntity);
        attemptEntity.setStartedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Upload attachment validation tests")
    class UploadValidationTests {

        @Test
        @DisplayName("Should throw exception when file is null")
        void shouldThrowWhenFileIsNull() {
            assertThatThrownBy(() ->
                    attachmentService.uploadAttachment(10L, 5L, 100L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("File is required");
        }

        @Test
        @DisplayName("Should throw exception when file is empty")
        void shouldThrowWhenFileIsEmpty() {
            MultipartFile emptyFile = mock(MultipartFile.class);
            when(emptyFile.isEmpty()).thenReturn(true);

            assertThatThrownBy(() ->
                    attachmentService.uploadAttachment(10L, 5L, 100L, emptyFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("File is required");
        }

        @Test
        @DisplayName("Should throw exception when file exceeds max size")
        void shouldThrowWhenFileTooLarge() {
            MultipartFile largeFile = mock(MultipartFile.class);
            when(largeFile.isEmpty()).thenReturn(false);
            when(largeFile.getSize()).thenReturn(20_000_000L);

            assertThatThrownBy(() ->
                    attachmentService.uploadAttachment(10L, 5L, 100L, largeFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds maximum");
        }

        @Test
        @DisplayName("Should throw exception when file type not allowed")
        void shouldThrowWhenFileTypeNotAllowed() {
            MultipartFile exeFile = mock(MultipartFile.class);
            when(exeFile.isEmpty()).thenReturn(false);
            when(exeFile.getSize()).thenReturn(1000L);
            when(exeFile.getContentType()).thenReturn("application/x-msdownload");

            assertThatThrownBy(() ->
                    attachmentService.uploadAttachment(10L, 5L, 100L, exeFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("File type not allowed");
        }

        @Test
        @DisplayName("Should throw exception when attempt not found")
        void shouldThrowWhenAttemptNotFound() {
            MultipartFile file = createValidMockFile();
            when(testAttemptRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    attachmentService.uploadAttachment(10L, 5L, 100L, file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Test attempt not found");
        }

        @Test
        @DisplayName("Should throw exception when attempt is finished")
        void shouldThrowWhenAttemptFinished() {
            attemptEntity.setFinishedAt(LocalDateTime.now());
            MultipartFile file = createValidMockFile();
            when(testAttemptRepository.findById(10L)).thenReturn(Optional.of(attemptEntity));

            assertThatThrownBy(() ->
                    attachmentService.uploadAttachment(10L, 5L, 100L, file))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("finished test attempt");
        }

        @Test
        @DisplayName("Should throw exception when user doesn't own attempt")
        void shouldThrowWhenUserDoesNotOwnAttempt() {
            UserEntity otherUser = new UserEntity();
            otherUser.setId(999L);
            attemptEntity.setStudent(otherUser);

            MultipartFile file = createValidMockFile();
            when(testAttemptRepository.findById(10L)).thenReturn(Optional.of(attemptEntity));

            assertThatThrownBy(() ->
                    attachmentService.uploadAttachment(10L, 5L, 100L, file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not own");
        }

        @Test
        @DisplayName("Should throw exception when max attachments reached")
        void shouldThrowWhenMaxAttachmentsReached() {
            MultipartFile file = createValidMockFile();
            when(testAttemptRepository.findById(10L)).thenReturn(Optional.of(attemptEntity));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(createAssignmentEntity()));
            when(attachmentRepository.countByAttemptIdAndAssignmentId(10L, 5L)).thenReturn(5L);

            assertThatThrownBy(() ->
                    attachmentService.uploadAttachment(10L, 5L, 100L, file))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Maximum number of attachments");
        }
    }

    @Nested
    @DisplayName("Upload attachment success tests")
    class UploadSuccessTests {

        @Test
        @DisplayName("Should successfully upload valid attachment")
        void shouldUploadValidAttachment() throws IOException {
            MultipartFile file = createValidMockFile();
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));

            when(testAttemptRepository.findById(10L)).thenReturn(Optional.of(attemptEntity));
            when(assignmentRepository.findById(5L)).thenReturn(Optional.of(createAssignmentEntity()));
            when(attachmentRepository.countByAttemptIdAndAssignmentId(10L, 5L)).thenReturn(0L);
            when(userRepository.findById(100L)).thenReturn(Optional.of(userEntity));
            when(attachmentRepository.save(any())).thenAnswer(invocation -> {
                AttachmentEntity saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            AttachmentEntity result = attachmentService.uploadAttachment(10L, 5L, 100L, file);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getOriginalFilename()).isEqualTo("test.pdf");
            assertThat(result.getContentType()).isEqualTo("application/pdf");
            assertThat(result.getTestAttempt()).isEqualTo(attemptEntity);
            assertThat(result.getUploadedBy()).isEqualTo(userEntity);
            verify(attachmentRepository).save(any(AttachmentEntity.class));
        }
    }

    @Nested
    @DisplayName("Get attachment tests")
    class GetAttachmentTests {

        @Test
        @DisplayName("Should return attachment when found")
        void shouldReturnAttachmentWhenFound() {
            AttachmentEntity attachment = AttachmentEntity.builder()
                    .originalFilename("test.pdf")
                    .storedFilename("uuid.pdf")
                    .contentType("application/pdf")
                    .fileSize(1000L)
                    .storagePath("/path/to/file.pdf")
                    .testAttempt(attemptEntity)
                    .assignmentId(5L)
                    .uploadedBy(userEntity)
                    .build();
            attachment.setId(1L);

            when(attachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));

            AttachmentEntity result = attachmentService.getAttachment(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getOriginalFilename()).isEqualTo("test.pdf");
        }

        @Test
        @DisplayName("Should throw exception when attachment not found")
        void shouldThrowWhenAttachmentNotFound() {
            when(attachmentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> attachmentService.getAttachment(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Attachment not found");
        }
    }

    @Nested
    @DisplayName("Get attachments for answer tests")
    class GetAttachmentsForAnswerTests {

        @Test
        @DisplayName("Should return attachments for answer")
        void shouldReturnAttachmentsForAnswer() {
            AttachmentEntity attachment1 = createAttachmentEntity(1L, "file1.pdf");
            AttachmentEntity attachment2 = createAttachmentEntity(2L, "file2.pdf");

            when(attachmentRepository.findByAttemptIdAndAssignmentId(10L, 5L))
                    .thenReturn(List.of(attachment1, attachment2));

            List<AttachmentEntity> result = attachmentService.getAttachmentsForAnswer(10L, 5L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getOriginalFilename()).isEqualTo("file1.pdf");
            assertThat(result.get(1).getOriginalFilename()).isEqualTo("file2.pdf");
        }

        @Test
        @DisplayName("Should return empty list when no attachments")
        void shouldReturnEmptyListWhenNoAttachments() {
            when(attachmentRepository.findByAttemptIdAndAssignmentId(10L, 5L)).thenReturn(List.of());

            List<AttachmentEntity> result = attachmentService.getAttachmentsForAnswer(10L, 5L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Delete attachment tests")
    class DeleteAttachmentTests {

        @Test
        @DisplayName("Should throw exception when user doesn't own attachment")
        void shouldThrowWhenUserDoesNotOwnAttachment() {
            UserEntity otherUser = new UserEntity();
            otherUser.setId(999L);

            AttachmentEntity attachment = createAttachmentEntity(1L, "test.pdf");
            attachment.setUploadedBy(otherUser);

            when(attachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));

            assertThatThrownBy(() -> attachmentService.deleteAttachment(1L, 100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not own");
        }

        @Test
        @DisplayName("Should throw exception when attempt is finished")
        void shouldThrowWhenDeletingFromFinishedAttempt() {
            attemptEntity.setFinishedAt(LocalDateTime.now());
            AttachmentEntity attachment = createAttachmentEntity(1L, "test.pdf");

            when(attachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));

            assertThatThrownBy(() -> attachmentService.deleteAttachment(1L, 100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("finished test attempt");
        }

        @Test
        @DisplayName("Should successfully delete attachment")
        void shouldDeleteAttachment() throws IOException {
            Path testFile = tempDir.resolve("test-file.pdf");
            java.nio.file.Files.createFile(testFile);

            AttachmentEntity attachment = createAttachmentEntity(1L, "test.pdf");
            attachment.setStoragePath(testFile.toString());

            when(attachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));

            attachmentService.deleteAttachment(1L, 100L);

            verify(attachmentRepository).delete(attachment);
            assertThat(testFile).doesNotExist();
        }
    }

    private MultipartFile createValidMockFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1000L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        return file;
    }

    private OpenQuestionAssignmentEntityEntity createAssignmentEntity() {
        OpenQuestionAssignmentEntityEntity assignment = new OpenQuestionAssignmentEntityEntity();
        assignment.setId(5L);
        assignment.setTestEntity(testEntity);
        return assignment;
    }

    private AttachmentEntity createAttachmentEntity(Long id, String filename) {
        AttachmentEntity attachment = AttachmentEntity.builder()
                .originalFilename(filename)
                .storedFilename("uuid-" + filename)
                .contentType("application/pdf")
                .fileSize(1000L)
                .storagePath(tempDir.resolve("uuid-" + filename).toString())
                .testAttempt(attemptEntity)
                .assignmentId(5L)
                .uploadedBy(userEntity)
                .build();
        attachment.setId(id);
        return attachment;
    }
}
