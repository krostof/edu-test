package com.edutest.commons.security;

import com.edutest.api.model.UserSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class LoginUserDetailsService implements UserDetailsService {

    private final LoginAndRegisterFacade loginAndRegisterFacade;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return loginAndRegisterFacade.findUserByUsername(username)
                .map(UserPrincipal::create)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public static class UserPrincipal implements UserDetails {
        private final String id;
        private final String username;
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
            List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getValue())
            );

            return new UserPrincipal(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                authorities,
                user.getIsActive()
            );
        }

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
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