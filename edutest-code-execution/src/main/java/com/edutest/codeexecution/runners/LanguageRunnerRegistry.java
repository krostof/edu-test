package com.edutest.codeexecution.runners;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LanguageRunnerRegistry {

    private final Map<String, LanguageRunner> runners;

    public LanguageRunnerRegistry(List<LanguageRunner> runnerBeans) {
        this.runners = runnerBeans.stream()
                .collect(Collectors.toMap(r -> normalize(r.language()), Function.identity()));
    }

    public LanguageRunner resolve(String language) {
        if (language == null) {
            throw new UnsupportedLanguageException("null");
        }
        LanguageRunner runner = runners.get(normalize(language));
        if (runner == null) {
            throw new UnsupportedLanguageException(language);
        }
        return runner;
    }

    public boolean supports(String language) {
        return language != null && runners.containsKey(normalize(language));
    }

    private static String normalize(String language) {
        String trimmed = language.trim().toLowerCase(Locale.ROOT);
        return switch (trimmed) {
            case "c++", "cplusplus" -> "cpp";
            case "js", "node", "nodejs" -> "javascript";
            case "py", "python3" -> "python";
            default -> trimmed;
        };
    }
}
