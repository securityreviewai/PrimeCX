package com.primecx.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.primecx.dto.CannedResponseDto;
import com.primecx.dto.CreateCannedResponseRequest;
import com.primecx.dto.UpdateCannedResponseRequest;
import com.primecx.model.CannedResponse;
import com.primecx.model.Role;
import com.primecx.model.User;
import com.primecx.service.CannedResponseService;
import com.primecx.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/canned-responses")
@RequiredArgsConstructor
public class CannedResponseController {

    private final CannedResponseService cannedResponseService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<CannedResponseDto>> listAll(
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        enforceAgentOrAbove(currentUser);

        List<CannedResponseDto> dtos = cannedResponseService.getAll().stream()
                .map(cannedResponseService::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CannedResponseDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        enforceAgentOrAbove(currentUser);

        CannedResponse cr = cannedResponseService.getById(id);
        return ResponseEntity.ok(cannedResponseService.toDto(cr));
    }

    @PostMapping
    public ResponseEntity<CannedResponseDto> create(
            @Valid @RequestBody CreateCannedResponseRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        enforceAdmin(currentUser);

        CannedResponse cr = cannedResponseService.create(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(cannedResponseService.toDto(cr));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CannedResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCannedResponseRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        enforceAdmin(currentUser);

        CannedResponse cr = cannedResponseService.update(id, request);
        return ResponseEntity.ok(cannedResponseService.toDto(cr));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        enforceAdmin(currentUser);

        cannedResponseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void enforceAgentOrAbove(User user) {
        Role role = user.getRole();
        if (role != Role.ROLE_SUPPORT_EXECUTIVE
                && role != Role.ROLE_SUPPORT_ADMIN
                && role != Role.ROLE_SUPPORT_MANAGER) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only support agents and above can access canned responses");
        }
    }

    private void enforceAdmin(User user) {
        if (user.getRole() != Role.ROLE_SUPPORT_ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only admins can modify canned responses");
        }
    }
}
