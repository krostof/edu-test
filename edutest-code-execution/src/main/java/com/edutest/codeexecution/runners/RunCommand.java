package com.edutest.codeexecution.runners;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RunCommand {

    private final String image;
    private final String sourceFilename;
    private final String[] compileCmd;
    private final String[] runCmd;

    public boolean requiresCompilation() {
        return compileCmd != null && compileCmd.length > 0;
    }
}
