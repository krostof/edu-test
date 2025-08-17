package com.edutest.persistance.entity.code;

import lombok.Getter;

@Getter
public enum CompilationStatus {
    NOT_COMPILED("Not Compiled"),
    SUCCESS("Compilation Successful"),
    ERROR("Compilation Error"),
    TIMEOUT("Compilation Timeout");

    private final String displayName;

    CompilationStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean isSuccessful() {
        return this == SUCCESS;
    }

    public boolean hasError() {
        return this == ERROR;
    }
}
