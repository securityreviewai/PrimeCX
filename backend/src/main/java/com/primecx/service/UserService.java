package com.primecx.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.UpdateProfileRequest;
import com.primecx.dto.UserDto;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Role;
import com.primecx.model.User;
import com.primecx.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public User getUserByOktaId(String oktaId) {
        return userRepository.findByOktaId(oktaId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with oktaId: " + oktaId));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional
    public User createOrUpdateFromOidc(OidcUser oidcUser) {
        String oktaId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String firstName = oidcUser.getGivenName();
        String lastName = oidcUser.getFamilyName();

        return userRepository.findByOktaId(oktaId)
                .map(existing -> {
                    existing.setEmail(email);
                    existing.setFirstName(firstName != null ? firstName : existing.getFirstName());
                    existing.setLastName(lastName != null ? lastName : existing.getLastName());
                    existing.setUpdatedAt(LocalDateTime.now());
                    log.debug("Updated existing user from OIDC: {}", email);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setOktaId(oktaId);
                    newUser.setEmail(email);
                    newUser.setFirstName(firstName);
                    newUser.setLastName(lastName);
                    newUser.setRole(Role.ROLE_USER);
                    newUser.setActive(true);
                    newUser.setCreatedAt(LocalDateTime.now());
                    newUser.setUpdatedAt(LocalDateTime.now());
                    log.info("Created new user from OIDC: {}", email);
                    return userRepository.save(newUser);
                });
    }

    @Transactional
    public User updateUserRole(Long userId, Role role) {
        User user = getUserById(userId);
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public User deactivateUser(Long userId) {
        User user = getUserById(userId);
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isActive()
        );
    }
}
