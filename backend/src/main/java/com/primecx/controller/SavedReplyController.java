package com.primecx.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.CreateSavedReplyRequest;
import com.primecx.dto.SavedReplyDto;
import com.primecx.dto.UpdateSavedReplyRequest;
import com.primecx.model.User;
import com.primecx.service.SavedReplyService;
import com.primecx.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/saved-replies")
@RequiredArgsConstructor
public class SavedReplyController {

    private final SavedReplyService savedReplyService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPPORT_EXECUTIVE', 'SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<List<SavedReplyDto>> list() {
        return ResponseEntity.ok(savedReplyService.listAll());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SUPPORT_EXECUTIVE', 'SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<List<SavedReplyDto>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(savedReplyService.search(q, limit));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<SavedReplyDto> create(
            @Valid @RequestBody CreateSavedReplyRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        SavedReplyDto created = savedReplyService.create(actor, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<SavedReplyDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSavedReplyRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(savedReplyService.update(actor, id, request));
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        savedReplyService.delete(actor, id);
        return ResponseEntity.noContent().build();
    }
}
