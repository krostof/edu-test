package com.edutest.codeexecution.runners;

public interface LanguageRunner {

    String language();

    RunCommand buildRunCommand(int memoryLimitMb);
}
