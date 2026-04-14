package com.edutest.service.attachment;

import com.edutest.persistance.entity.attachment.AttachmentEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.AssignmentJpaRepository;
import com.edutest.persistance.repository.AttachmentRepository;
import com.edutest.persistance.repository.TestAttemptJpaRepository;
import com.edutest.persistance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TestAttemptJpaRepository testAttemptRepository;
    private final AssignmentJpaRepository assignmentRepository;
    private final UserRepository userRepository;

    @Value("${app.attachment.storage-path:./uploads}")
    private String storagePath;

    @Value("${app.attachment.max-file-size:10485760}")
    private long maxFileSize; // 10MB default

    @Value("${app.attachment.max-attachments-per-answer:5}")
    private int maxAttachmentsPerAnswer;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/rtf"
    );

    @Transactional
    public AttachmentEntity uploadAttachment(
            Long attemptId,
            Long assignmentId,
            Long userId,
            MultipartFile file) throws IOException {

        log.info("Uploading attachment for attempt {} assignment {} by user {}",
                attemptId, assignmentId, userId);

        // Validate file
        validateFile(file);

        // Validate attempt and assignment
        TestAttemptEntity attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Test attempt not found: " + attemptId));

        if (attempt.isFinished()) {
            throw new IllegalStateException("Cannot upload attachments to a finished test attempt");
        }

        if (!attempt.getStudent().getId().equals(userId)) {
            throw new IllegalArgumentException("User does not own this test attempt");
        }

        // Validate assignment belongs to test
        boolean assignmentBelongsToTest = assignmentRepository.findById(assignmentId)
                .map(a -> a.getTestEntity().getId().equals(attempt.getTestEntity().getId()))
                .orElse(false);

        if (!assignmentBelongsToTest) {
            throw new IllegalArgumentException("Assignment does not belong to this test");
        }

        // Check attachment limit
        long currentCount = attachmentRepository.countByAttemptIdAndAssignmentId(attemptId, assignmentId);
        if (currentCount >= maxAttachmentsPerAnswer) {
            throw new IllegalStateException("Maximum number of attachments (" + maxAttachmentsPerAnswer +
                    ") reached for this answer");
        }

        // Check if attachments are allowed for this assignment
        // Note: For now we allow all assignments, but could add validation here

        // Create storage directory if not exists
        Path uploadDir = Paths.get(storagePath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);

        // Store file
        Path targetPath = uploadDir.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Get user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Create entity
        AttachmentEntity attachment = AttachmentEntity.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .storagePath(targetPath.toString())
                .testAttempt(attempt)
                .assignmentId(assignmentId)
                .uploadedBy(user)
                .build();

        AttachmentEntity saved = attachmentRepository.save(attachment);
        log.info("Saved attachment with id {}", saved.getId());

        return saved;
    }

    @Transactional(readOnly = true)
    public AttachmentEntity getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));
    }

    @Transactional(readOnly = true)
    public Resource loadAttachmentAsResource(Long attachmentId) {
        AttachmentEntity attachment = getAttachment(attachmentId);

        try {
            Path filePath = Paths.get(attachment.getStoragePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalStateException("Could not read attachment file: " + attachment.getOriginalFilename());
            }
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not read attachment file: " + attachment.getOriginalFilename(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<AttachmentEntity> getAttachmentsForAnswer(Long attemptId, Long assignmentId) {
        return attachmentRepository.findByAttemptIdAndAssignmentId(attemptId, assignmentId);
    }

    @Transactional(readOnly = true)
    public List<AttachmentEntity> getAttachmentsForAttempt(Long attemptId) {
        return attachmentRepository.findByAttemptId(attemptId);
    }

    @Transactional
    public void deleteAttachment(Long attachmentId, Long userId) {
        AttachmentEntity attachment = getAttachment(attachmentId);

        // Verify ownership
        if (!attachment.getUploadedBy().getId().equals(userId)) {
            throw new IllegalArgumentException("User does not own this attachment");
        }

        // Verify attempt is not finished
        if (attachment.getTestAttempt().isFinished()) {
            throw new IllegalStateException("Cannot delete attachments from a finished test attempt");
        }

        // Delete file from storage
        try {
            Path filePath = Paths.get(attachment.getStoragePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Could not delete attachment file: {}", attachment.getStoragePath(), e);
        }

        // Delete entity
        attachmentRepository.delete(attachment);
        log.info("Deleted attachment {}", attachmentId);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed (" +
                    (maxFileSize / 1024 / 1024) + " MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("File type not allowed: " + contentType +
                    ". Allowed types: images, PDF, Word documents, text files");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
