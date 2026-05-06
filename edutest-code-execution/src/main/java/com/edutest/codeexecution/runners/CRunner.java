package com.edutest.codeexecution.runners;

import org.springframework.stereotype.Component;

@Component
public class CRunner implements LanguageRunner {

    @Override
    public String language() {
        return "c";
    }

    @Override
    public RunCommand buildRunCommand(int memoryLimitMb) {
        return RunCommand.builder()
                .image("gcc:13")
                .sourceFilename("main.c")
                .compileCmd(new String[]{"gcc", "-O2", "-o", "/tmp/sol", "/workspace/main.c"})
                .runCmd(new String[]{"/tmp/sol"})
                .build();
    }
}
