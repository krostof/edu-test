package com.edutest.codeexecution.runners;

import org.springframework.stereotype.Component;

@Component
public class PythonRunner implements LanguageRunner {

    @Override
    public String language() {
        return "python";
    }

    @Override
    public RunCommand buildRunCommand(int memoryLimitMb) {
        return RunCommand.builder()
                .image("python:3.12-alpine")
                .sourceFilename("main.py")
                .runCmd(new String[]{"python", "/workspace/main.py"})
                .build();
    }
}
