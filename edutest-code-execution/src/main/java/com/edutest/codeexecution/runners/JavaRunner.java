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
        // eclipse-temurin replaces the unmaintained `openjdk:*` images, which were retired
        // from Docker Hub in 2024. Alpine variant keeps the image small (~200 MB).
        // Class name `Solution` matches the LeetCode/HackerRank convention students are
        // most likely to recognise; the file name must match because Java requires the file
        // to share the public class's name.
        //
        // Compiled .class goes to /tmp, not /workspace: the workspace tmpfs is mounted with
        // default 0755 owned by root (only the source file is written there as root by the
        // executor), while /tmp gets the conventional 1777 sticky bit so the unprivileged
        // `nobody` user can write the class file there.
        return RunCommand.builder()
                .image("eclipse-temurin:21-jdk-alpine")
                .sourceFilename("Solution.java")
                .compileCmd(new String[]{"javac", "/workspace/Solution.java", "-d", "/tmp"})
                .runCmd(new String[]{"java", "-Xmx" + memoryLimitMb + "m", "-cp", "/tmp", "Solution"})
                .build();
    }
}
