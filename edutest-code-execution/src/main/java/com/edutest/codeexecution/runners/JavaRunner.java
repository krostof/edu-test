package com.edutest.codeexecution.runners;

import org.springframework.stereotype.Component;

@Component
public class JavaRunner implements LanguageRunner {

    @Override
    public String language() {
        return "java";
    }

    @Override
    public RunCommand buildRunCommand(int memoryLimitMb) {
        return RunCommand.builder()
                .image("openjdk:21-slim")
                .sourceFilename("Main.java")
                .compileCmd(new String[]{"javac", "/workspace/Main.java", "-d", "/workspace"})
                .runCmd(new String[]{"java", "-Xmx" + memoryLimitMb + "m", "-cp", "/workspace", "Main"})
                .build();
    }
}
