package com.edutest.codeexecution.runners;

import org.springframework.stereotype.Component;

/**
 * Runs C# code using Mono — chosen over the official .NET SDK image because:
 *  - mono image is ~150 MB vs ~250 MB for dotnet/sdk
 *  - mcs compiles single .cs files directly (no .csproj boilerplate)
 *  - faster cold start (dotnet SDK runs NuGet restore on first build)
 *
 * Tradeoff: Mono lags ~1-2 years behind official .NET in language features.
 * Acceptable for educational use; switch to dotnet/sdk if students need C# 12+.
 */
@Component
public class CSharpRunner implements LanguageRunner {

    @Override
    public String language() {
        return "csharp";
    }

    @Override
    public RunCommand buildRunCommand(int memoryLimitMb) {
        return RunCommand.builder()
                .image("mono:6.12-slim")
                .sourceFilename("Main.cs")
                .compileCmd(new String[]{"mcs", "-optimize", "-out:/tmp/sol.exe", "/workspace/Main.cs"})
                .runCmd(new String[]{"mono", "/tmp/sol.exe"})
                .build();
    }
}
