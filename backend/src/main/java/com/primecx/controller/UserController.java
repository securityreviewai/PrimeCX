package com.primecx.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.CreateUserRequest;
import com.primecx.dto.PagedResponse;
import com.primecx.dto.UpdateRoleRequest;
import com.primecx.dto.UpdateUserRequest;
import com.primecx.dto.UserDto;
import com.primecx.model.Role;
import com.primecx.model.User;
import com.primecx.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal OidcUser oidcUser) {
        User user = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(userService.toDto(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(
            @AuthenticationPrincipal OidcUser oidcUser,
            @Valid @RequestBody UpdateUserRequest request) {
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        User updated = userService.updateUser(actor.getId(), request, actor.getId());
        return ResponseEntity.ok(userService.toDto(updated));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<PagedResponse<UserDto>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getPagedUsers(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<PagedResponse<UserDto>> searchUsers(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.searchUsers(q, pageable));
    }

    @GetMapping("/filter/active")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<PagedResponse<UserDto>> getUsersByActive(
            @RequestParam boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getPagedUsersByActive(active, pageable));
    }

    @GetMapping("/filter/role")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<PagedResponse<UserDto>> getUsersByRole(
            @RequestParam Role role,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getPagedUsersByRole(role, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER') or @userSecurity.isSelf(#id, authentication)")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toDto(userService.getUserById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPPORT_ADMIN')")
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        User created = userService.createUser(request, actor.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.toDto(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPPORT_ADMIN') or @userSecurity.isSelf(#id, authentication)")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        User updated = userService.updateUser(id, request, actor.getId());
        return ResponseEntity.ok(userService.toDto(updated));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('SUPPORT_ADMIN')")
    public ResponseEntity<UserDto> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(userService.toDto(userService.updateUserRole(id, request.role(), actor.getId())));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPPORT_ADMIN')")
    public ResponseEntity<UserDto> deactivateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(userService.toDto(userService.deactivateUser(id, actor.getId())));
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('SUPPORT_ADMIN')")
    public ResponseEntity<UserDto> reactivateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(userService.toDto(userService.reactivateUser(id, actor.getId())));
    }

    @PutMapping("/{id}/organization")
    @PreAuthorize("hasRole('SUPPORT_ADMIN')")
    public ResponseEntity<UserDto> assignOrganization(
            @PathVariable Long id,
            @RequestParam Long organizationId,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(userService.toDto(userService.assignOrganization(id, organizationId, actor.getId())));
    }
}
