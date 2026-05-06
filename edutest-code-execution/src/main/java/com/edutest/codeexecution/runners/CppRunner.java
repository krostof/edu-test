package com.edutest.codeexecution.runners;

import org.springframework.stereotype.Component;

@Component
public class CppRunner implements LanguageRunner {

    @Override
    public String language() {
        return "cpp";
    }

    @Override
    public RunCommand buildRunCommand(int memoryLimitMb) {
        return RunCommand.builder()
                .image("gcc:13")
                .sourceFilename("main.cpp")
                .compileCmd(new String[]{"g++", "-O2", "-o", "/tmp/sol", "/workspace/main.cpp"})
                .runCmd(new String[]{"/tmp/sol"})
                .build();
    }
}
