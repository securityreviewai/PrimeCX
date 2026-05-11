package com.primecx.controller;

import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.CreateSessionRequest;
import com.primecx.dto.SupportSessionDto;
import com.primecx.model.Role;
import com.primecx.model.SupportSession;
import com.primecx.model.User;
import com.primecx.service.SupportSessionService;
import com.primecx.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SupportSessionController {

    private final SupportSessionService supportSessionService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('SUPPORT_EXECUTIVE')")
    public ResponseEntity<SupportSessionDto> startSession(
            @RequestBody CreateSessionRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User executive = userService.getUserByOktaId(oidcUser.getSubject());
        SupportSession session = supportSessionService.startSession(request, executive.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(supportSessionService.toDto(session));
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<SupportSessionDto> endSession(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal OidcUser oidcUser) {
        String notes = body.get("notes");
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        SupportSession session = supportSessionService.endSession(id, notes, actor.getId());
        return ResponseEntity.ok(supportSessionService.toDto(session));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<SupportSessionDto> cancelSession(@PathVariable Long id) {
        SupportSession session = supportSessionService.cancelSession(id);
        return ResponseEntity.ok(supportSessionService.toDto(session));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupportSessionDto> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(supportSessionService.toDto(supportSessionService.getSessionById(id)));
    }

    @GetMapping
    public ResponseEntity<List<SupportSessionDto>> getSessions(@AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        List<SupportSession> sessions;

        if (currentUser.getRole() == Role.ROLE_SUPPORT_ADMIN || currentUser.getRole() == Role.ROLE_SUPPORT_MANAGER) {
            sessions = supportSessionService.getActiveSessions();
        } else if (currentUser.getRole() == Role.ROLE_SUPPORT_EXECUTIVE) {
            sessions = supportSessionService.getSessionsByExecutive(currentUser.getId());
        } else {
            sessions = supportSessionService.getSessionsByUser(currentUser.getId());
        }

        List<SupportSessionDto> dtos = sessions.stream().map(supportSessionService::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/active")
    public ResponseEntity<List<SupportSessionDto>> getActiveSessions() {
        List<SupportSessionDto> dtos = supportSessionService.getActiveSessions().stream()
                .map(supportSessionService::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }
}
