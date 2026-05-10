package com.edutest.codeexecution.runners;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RunnersTest {

    @Test
    @DisplayName("Python: no compile step, runs python on /workspace/main.py")
    void pythonRunner() {
        RunCommand cmd = new PythonRunner().buildRunCommand(256);

        assertThat(cmd.getImage()).isEqualTo("python:3.12-alpine");
        assertThat(cmd.getSourceFilename()).isEqualTo("main.py");
        assertThat(cmd.requiresCompilation()).isFalse();
        assertThat(cmd.getRunCmd()).containsExactly("python", "/workspace/main.py");
    }

    @Test
    @DisplayName("JavaScript: no compile step, runs node on /workspace/main.js")
    void javascriptRunner() {
        RunCommand cmd = new JavascriptRunner().buildRunCommand(256);

        assertThat(cmd.getImage()).isEqualTo("node:20-alpine");
        assertThat(cmd.getSourceFilename()).isEqualTo("main.js");
        assertThat(cmd.requiresCompilation()).isFalse();
        assertThat(cmd.getRunCmd()).containsExactly("node", "/workspace/main.js");
    }

    @Test
    @DisplayName("Java: javac compile to /workspace, java -Xmx<mem>m for run")
    void javaRunner() {
        RunCommand cmd = new JavaRunner().buildRunCommand(384);

        assertThat(cmd.getImage()).isEqualTo("eclipse-temurin:21-jdk-alpine");
        assertThat(cmd.getSourceFilename()).isEqualTo("Solution.java");
        assertThat(cmd.requiresCompilation()).isTrue();
        assertThat(cmd.getCompileCmd()).containsExactly("javac", "/workspace/Solution.java", "-d", "/tmp");
        assertThat(cmd.getRunCmd()).containsExactly("java", "-Xmx384m", "-cp", "/tmp", "Solution");
    }

    @Test
    @DisplayName("C++: g++ compile, runs /tmp/sol")
    void cppRunner() {
        RunCommand cmd = new CppRunner().buildRunCommand(256);

        assertThat(cmd.getImage()).isEqualTo("gcc:13");
        assertThat(cmd.getSourceFilename()).isEqualTo("main.cpp");
        assertThat(cmd.requiresCompilation()).isTrue();
        assertThat(cmd.getCompileCmd()).containsExactly("g++", "-O2", "-o", "/tmp/sol", "/workspace/main.cpp");
        assertThat(cmd.getRunCmd()).containsExactly("/tmp/sol");
    }

    @Test
    @DisplayName("C: gcc compile, runs /tmp/sol")
    void cRunner() {
        RunCommand cmd = new CRunner().buildRunCommand(256);

        assertThat(cmd.getImage()).isEqualTo("gcc:13");
        assertThat(cmd.getSourceFilename()).isEqualTo("main.c");
        assertThat(cmd.requiresCompilation()).isTrue();
        assertThat(cmd.getCompileCmd()).containsExactly("gcc", "-O2", "-o", "/tmp/sol", "/workspace/main.c");
        assertThat(cmd.getRunCmd()).containsExactly("/tmp/sol");
    }

    @Test
    @DisplayName("C#: mcs compile via Mono, mono runs /tmp/sol.exe")
    void csharpRunner() {
        RunCommand cmd = new CSharpRunner().buildRunCommand(256);

        assertThat(cmd.getImage()).isEqualTo("mono:6.12");
        assertThat(cmd.getSourceFilename()).isEqualTo("Main.cs");
        assertThat(cmd.requiresCompilation()).isTrue();
        assertThat(cmd.getCompileCmd()).containsExactly(
                "mcs", "-optimize", "-out:/tmp/sol.exe", "/workspace/Main.cs");
        assertThat(cmd.getRunCmd()).containsExactly("mono", "/tmp/sol.exe");
    }
}
