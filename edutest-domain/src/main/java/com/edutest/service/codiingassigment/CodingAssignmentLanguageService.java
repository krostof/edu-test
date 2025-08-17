package com.edutest.service.codiingassigment;

import com.edutest.domain.assignment.coding.CodingAssignment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CodingAssignmentLanguageService {

    public CodingAssignment addAllowedLanguage(CodingAssignment assignment, String language) {
        List<String> current = new ArrayList<>(assignment.getAllowedLanguagesList());

        if (!current.contains(language)) {
            current.add(language);
            String newLanguagesString = String.join(",", current);

            return assignment.toBuilder()
                    .allowedLanguages(newLanguagesString)
                    .build();
        }

        return assignment;
    }

    public CodingAssignment removeAllowedLanguage(CodingAssignment assignment, String language) {
        List<String> current = new ArrayList<>(assignment.getAllowedLanguagesList());
        current.remove(language);

        String newLanguagesString = current.isEmpty() ? null : String.join(",", current);

        return assignment.toBuilder()
                .allowedLanguages(newLanguagesString)
                .build();
    }

    public CodingAssignment setAllowedLanguages(CodingAssignment assignment, List<String> languages) {
        String languagesString = (languages == null || languages.isEmpty()) ?
                null : String.join(",", languages);

        return assignment.toBuilder()
                .allowedLanguages(languagesString)
                .build();
    }
}
