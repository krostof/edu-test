package com.edutest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
    private Long id;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private Long attemptId;
    private Long assignmentId;
    private Long uploadedById;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
}
