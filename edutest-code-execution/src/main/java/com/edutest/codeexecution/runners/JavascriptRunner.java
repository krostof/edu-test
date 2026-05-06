package com.edutest.codeexecution.runners;

import org.springframework.stereotype.Component;

@Component
public class JavascriptRunner implements LanguageRunner {

    @Override
    public String language() {
        return "javascript";
    }

    @Override
    public RunCommand buildRunCommand(int memoryLimitMb) {
        return RunCommand.builder()
                .image("node:20-alpine")
                .sourceFilename("main.js")
                .runCmd(new String[]{"node", "/workspace/main.js"})
                .build();
    }
}
