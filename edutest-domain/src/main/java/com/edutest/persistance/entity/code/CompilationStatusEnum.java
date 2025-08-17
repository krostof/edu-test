package com.edutest.persistance.entity.code;


public enum CompilationStatusEnum {
    NOT_COMPILED("Not Compiled"),
    SUCCESS("Compilation Successful"),
    ERROR("Compilation Error"),
    TIMEOUT("Compilation Timeout");

    private final String displayName;

    CompilationStatusEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

