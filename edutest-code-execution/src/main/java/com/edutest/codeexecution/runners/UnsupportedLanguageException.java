package com.edutest.codeexecution.runners;

public class UnsupportedLanguageException extends IllegalArgumentException {

    public UnsupportedLanguageException(String language) {
        super("Unsupported programming language: " + language);
    }
}
