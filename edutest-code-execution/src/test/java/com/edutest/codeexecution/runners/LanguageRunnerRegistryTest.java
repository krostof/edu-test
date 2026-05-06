package com.edutest.codeexecution.runners;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LanguageRunnerRegistryTest {

    private LanguageRunnerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new LanguageRunnerRegistry(List.of(
                new JavaRunner(),
                new PythonRunner(),
                new JavascriptRunner(),
                new CppRunner(),
                new CRunner()
        ));
    }

    @Test
    @DisplayName("Resolves canonical lowercase names")
    void resolvesCanonicalNames() {
        assertThat(registry.resolve("java")).isInstanceOf(JavaRunner.class);
        assertThat(registry.resolve("python")).isInstanceOf(PythonRunner.class);
        assertThat(registry.resolve("javascript")).isInstanceOf(JavascriptRunner.class);
        assertThat(registry.resolve("cpp")).isInstanceOf(CppRunner.class);
        assertThat(registry.resolve("c")).isInstanceOf(CRunner.class);
    }

    @Test
    @DisplayName("Normalises mixed-case input from frontend dropdown")
    void normalisesMixedCase() {
        assertThat(registry.resolve("Java")).isInstanceOf(JavaRunner.class);
        assertThat(registry.resolve("PYTHON")).isInstanceOf(PythonRunner.class);
        assertThat(registry.resolve("JavaScript")).isInstanceOf(JavascriptRunner.class);
    }

    @Test
    @DisplayName("Maps common aliases to canonical runners")
    void mapsAliases() {
        assertThat(registry.resolve("C++")).isInstanceOf(CppRunner.class);
        assertThat(registry.resolve("cplusplus")).isInstanceOf(CppRunner.class);
        assertThat(registry.resolve("py")).isInstanceOf(PythonRunner.class);
        assertThat(registry.resolve("python3")).isInstanceOf(PythonRunner.class);
        assertThat(registry.resolve("js")).isInstanceOf(JavascriptRunner.class);
        assertThat(registry.resolve("node")).isInstanceOf(JavascriptRunner.class);
        assertThat(registry.resolve("nodejs")).isInstanceOf(JavascriptRunner.class);
    }

    @Test
    @DisplayName("Trims surrounding whitespace before resolving")
    void trimsWhitespace() {
        assertThat(registry.resolve("  java  ")).isInstanceOf(JavaRunner.class);
    }

    @Test
    @DisplayName("Throws UnsupportedLanguageException for unknown language")
    void throwsForUnknownLanguage() {
        assertThatThrownBy(() -> registry.resolve("rust"))
                .isInstanceOf(UnsupportedLanguageException.class)
                .hasMessageContaining("rust");
    }

    @Test
    @DisplayName("Throws UnsupportedLanguageException for null")
    void throwsForNull() {
        assertThatThrownBy(() -> registry.resolve(null))
                .isInstanceOf(UnsupportedLanguageException.class);
    }

    @Test
    @DisplayName("supports() returns true only for known languages")
    void supportsKnownLanguages() {
        assertThat(registry.supports("Java")).isTrue();
        assertThat(registry.supports("C++")).isTrue();
        assertThat(registry.supports("rust")).isFalse();
        assertThat(registry.supports(null)).isFalse();
    }
}
