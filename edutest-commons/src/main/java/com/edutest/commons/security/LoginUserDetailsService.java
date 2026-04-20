package com.edutest.commons.security;

import com.edutest.api.model.UserRole;
import com.edutest.api.model.UserSecurity;
import com.edutest.service.security.LoginAndRegisterFacade;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class LoginUserDetailsService implements UserDetailsService {

    private final LoginAndRegisterFacade loginAndRegisterFacade;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var userOptional = loginAndRegisterFacade.findUserByUsername(username);
        if (userOptional.isEmpty()) {
            log.warn("User not found with username: {}", username);
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        UserSecurity user = userOptional.get();
        log.debug("Loading user: {}, roles: {}", username, user.getRoles());
        UserPrincipal principal = UserPrincipal.create(user);
        log.debug("User principal created with authorities: {}", principal.getAuthorities());
        return principal;
    }

    public static class UserPrincipal implements UserDetails {
        @Getter
        private final String id;
        private final String username;
        @Getter
        private final String email;
        private final String password;
        private final Collection<? extends GrantedAuthority> authorities;
        private final boolean isActive;

        public UserPrincipal(String id, String username, String email, String password,
                             Collection<? extends GrantedAuthority> authorities, boolean isActive) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.password = password;
            this.authorities = authorities;
            this.isActive = isActive;
        }

        public static UserPrincipal create(UserSecurity user) {
            List<GrantedAuthority> authorities;

            // Use roles list if available, otherwise fall back to single role
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                authorities = user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getValue()))
                        .collect(Collectors.toList());
            } else if (user.getRole() != null) {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getValue()));
            } else {
                authorities = new ArrayList<>();
            }

            return new UserPrincipal(
                    user.getId().toString(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getPassword(),
                    authorities,
                    user.getIsActive()
            );
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return isActive;
        }
    }
}