package com.edutest.webserver.api.helper;

import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityContextHelper {

    private final UserRepository userRepository;

    public UserEntity getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found: " + username));
    }

    public Long getCurrentUserId() {
        return getCurrentUserEntity().getId();
    }
}
