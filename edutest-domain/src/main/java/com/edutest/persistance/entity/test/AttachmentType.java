package com.edutest.persistance.entity.test;

import java.util.Arrays;
import java.util.List;

public enum AttachmentType {
    IMAGE("Image", Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "svg", "webp")),
    DOCUMENT("Document", Arrays.asList("pdf", "doc", "docx", "txt", "rtf", "odt")),
    ARCHIVE("Archive", Arrays.asList("zip", "rar", "7z", "tar", "gz")),
    OTHER("Other", Arrays.asList());

    private final String displayName;
    private final List<String> supportedExtensions;

    AttachmentType(String displayName, List<String> supportedExtensions) {
        this.displayName = displayName;
        this.supportedExtensions = supportedExtensions;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getSupportedExtensions() {
        return supportedExtensions;
    }

    public boolean supportsExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return supportedExtensions.contains(extension.toLowerCase());
    }

    public static AttachmentType fromExtension(String extension) {
        if (extension == null) {
            return OTHER;
        }

        String ext = extension.toLowerCase();
        for (AttachmentType type : values()) {
            if (type.supportsExtension(ext)) {
                return type;
            }
        }
        return OTHER;
    }

    public static AttachmentType fromMimeType(String mimeType) {
        if (mimeType == null) {
            return OTHER;
        }

        String type = mimeType.toLowerCase();
        if (type.startsWith("image/")) {
            return IMAGE;
        } else if (type.startsWith("video/")) {
            return VIDEO;
        } else if (type.startsWith("audio/")) {
            return AUDIO;
        } else if (type.equals("application/pdf") ||
                type.contains("document") ||
                type.contains("text")) {
            return DOCUMENT;
        } else if (type.contains("zip") || type.contains("archive")) {
            return ARCHIVE;
        }

        return OTHER;
    }
}
