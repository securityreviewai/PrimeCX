package com.primecx.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateUserRequest;
import com.primecx.dto.PagedResponse;
import com.primecx.dto.UpdateUserRequest;
import com.primecx.dto.UserDto;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Role;
import com.primecx.model.User;
import com.primecx.model.Organization;
import com.primecx.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final OrganizationService organizationService;

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User getUserByOktaId(String oktaId) {
        return userRepository.findByOktaId(oktaId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUserDtos() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getPagedUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDto> searchUsers(String query, Pageable pageable) {
        Page<User> page = userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        query, query, query, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getPagedUsersByActive(boolean active, Pageable pageable) {
        Page<User> page = userRepository.findByActive(active, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getPagedUsersByRole(Role role, Pageable pageable) {
        Page<User> page = userRepository.findByRole(role, pageable);
        return toPagedResponse(page);
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
    public User createUser(CreateUserRequest request, Long actorUserId) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }
        User user = User.builder()
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(Role.ROLE_USER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        User saved = userRepository.save(user);
        log.info("User created by actor {}: {}", actorUserId, saved.getId());
        return saved;
    }

    @Transactional
    public User updateUser(Long userId, UpdateUserRequest request, Long actorUserId) {
        User user = getUserById(userId);
        Map<String, String> changes = new LinkedHashMap<>();

        if (request.firstName() != null && !request.firstName().equals(user.getFirstName())) {
            changes.put("firstName", request.firstName());
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !request.lastName().equals(user.getLastName())) {
            changes.put("lastName", request.lastName());
            user.setLastName(request.lastName());
        }
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email already in use");
            }
            changes.put("email", request.email());
            user.setEmail(request.email());
        }

        if (!changes.isEmpty()) {
            user.setUpdatedAt(LocalDateTime.now());
            auditLogService.appendUserProfileUpdated(actorUserId, userId, changes);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserRole(Long userId, Role newRole, Long actorUserId) {
        User user = getUserById(userId);
        Role previous = user.getRole();
        user.setRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        auditLogService.appendRoleChanged(actorUserId, userId, previous, newRole);
        return saved;
    }

    @Transactional
    public User deactivateUser(Long userId, Long actorUserId) {
        User user = getUserById(userId);
        if (!user.isActive()) {
            throw new IllegalArgumentException("User is already deactivated");
        }
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        auditLogService.appendUserDeactivated(actorUserId, userId);
        return saved;
    }

    @Transactional
    public User reactivateUser(Long userId, Long actorUserId) {
        User user = getUserById(userId);
        if (user.isActive()) {
            throw new IllegalArgumentException("User is already active");
        }
        user.setActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        auditLogService.appendUserReactivated(actorUserId, userId);
        return saved;
    }

    @Transactional
    public User assignOrganization(Long userId, Long organizationId, Long actorUserId) {
        User user = getUserById(userId);
        Organization org = organizationService.getById(organizationId);
        user.setOrganization(org);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        auditLogService.appendUserOrganizationAssigned(actorUserId, userId, organizationId);
        return saved;
    }

    public UserDto toDto(User user) {
        Long orgId = null;
        String orgName = null;
        if (user.getOrganization() != null) {
            orgId = user.getOrganization().getId();
            orgName = user.getOrganization().getName();
        }
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isActive(),
                orgId,
                orgName
        );
    }

    private PagedResponse<UserDto> toPagedResponse(Page<User> page) {
        return new PagedResponse<>(
                page.getContent().stream().map(this::toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
