package com.edutest.domain.assignment;

import lombok.Getter;

@Getter
public enum AssignmentType {
    SINGLE_CHOICE("Single Choice"),
    MULTIPLE_CHOICE("Multiple Choice"),
    OPEN_QUESTION("Open Question"),
    CODING("Coding Assignment");

    private final String displayName;

    AssignmentType(String displayName) {
        this.displayName = displayName;
    }

    public boolean isAutoGradeable() {
        return this == SINGLE_CHOICE ||
                this == MULTIPLE_CHOICE ||
                this == CODING;
    }

    public boolean requiresManualGrading() {
        return this == OPEN_QUESTION;
    }

    public boolean supportsAttachments() {
        return this == SINGLE_CHOICE ||
                this == MULTIPLE_CHOICE ||
                this == OPEN_QUESTION;
    }
}
