package com.edutest.webserver.api.controller;

import com.edutest.commons.SecurityContextHelper;
import com.edutest.dto.AttachmentDto;
import com.edutest.persistance.entity.attachment.AttachmentEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.service.attachment.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final SecurityContextHelper securityContextHelper;

    @PostMapping("/tests/{testId}/attempts/{attemptId}/answers/{assignmentId}/attachments")
    public ResponseEntity<AttachmentDto> uploadAttachment(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            @PathVariable Long assignmentId,
            @RequestParam("file") MultipartFile file) throws IOException {

        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Upload attachment request: testId={}, attemptId={}, assignmentId={}, userId={}, filename={}",
                testId, attemptId, assignmentId, currentUser.getId(), file.getOriginalFilename());

        AttachmentEntity attachment = attachmentService.uploadAttachment(
                attemptId, assignmentId, currentUser.getId(), file);

        AttachmentDto dto = mapToDto(attachment);

        return ResponseEntity.status(201).body(dto);
    }

    @GetMapping("/tests/{testId}/attempts/{attemptId}/answers/{assignmentId}/attachments")
    public ResponseEntity<List<AttachmentDto>> getAttachments(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            @PathVariable Long assignmentId) {

        log.info("Get attachments: attemptId={}, assignmentId={}", attemptId, assignmentId);

        List<AttachmentEntity> attachments = attachmentService.getAttachmentsForAnswer(attemptId, assignmentId);

        List<AttachmentDto> dtos = attachments.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        log.info("Download attachment: attachmentId={}", attachmentId);

        AttachmentEntity attachment = attachmentService.getAttachment(attachmentId);
        Resource resource = attachmentService.loadAttachmentAsResource(attachmentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        UserEntity currentUser = securityContextHelper.getCurrentUserEntity();
        log.info("Delete attachment: attachmentId={}, userId={}", attachmentId, currentUser.getId());

        attachmentService.deleteAttachment(attachmentId, currentUser.getId());

        return ResponseEntity.noContent().build();
    }

    private AttachmentDto mapToDto(AttachmentEntity entity) {
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/attachments/")
                .path(entity.getId().toString())
                .toUriString();

        return AttachmentDto.builder()
                .id(entity.getId())
                .originalFilename(entity.getOriginalFilename())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .attemptId(entity.getTestAttempt().getId())
                .assignmentId(entity.getAssignmentId())
                .uploadedById(entity.getUploadedBy().getId())
                .uploadedAt(entity.getCreatedAt())
                .downloadUrl(downloadUrl)
                .build();
    }
}
