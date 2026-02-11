package com.edutest.service;

import com.edutest.commons.util.StringUtil;
import com.edutest.persistance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginGenerator {

    private final UserRepository userRepository;

    public String generateLogin(String firstName, String lastName) {
        String baseLogin = buildBaseLogin(firstName, lastName);
        return ensureUniqueLogin(baseLogin);
    }

    private String buildBaseLogin(String firstName, String lastName) {
        String sanitizedLastName = StringUtil.sanitize(lastName);
        String sanitizedFirstName = StringUtil.sanitize(firstName);

        return (sanitizedLastName + sanitizedFirstName.charAt(0)).toLowerCase();
    }

    private String ensureUniqueLogin(String baseLogin) {
        String userLogin = baseLogin;
        int suffix = 1;

        while (userRepository.findByUsername(userLogin).isPresent()) {
            userLogin = baseLogin + suffix;
            suffix++;
        }

        return userLogin;
    }

}
