package com.primecx.security;

import com.primecx.model.User;
import com.primecx.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {

    private final UserRepository userRepository;

    public boolean isSelf(Long userId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            return false;
        }
        String oktaId = oidcUser.getSubject();
        return userRepository.findById(userId)
                .map(user -> oktaId.equals(user.getOktaId()))
                .orElse(false);
    }
}
