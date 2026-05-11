package com.primecx.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.CreateTicketCommentRequest;
import com.primecx.dto.TicketCommentDto;
import com.primecx.dto.UpdateTicketCommentRequest;
import com.primecx.model.User;
import com.primecx.service.TicketCommentService;
import com.primecx.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class TicketCommentController {

    private final TicketCommentService ticketCommentService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<TicketCommentDto>> list(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(ticketCommentService.listComments(ticketId, currentUser));
    }

    @PostMapping
    public ResponseEntity<TicketCommentDto> create(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateTicketCommentRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        TicketCommentDto dto = ticketCommentService.createComment(ticketId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<TicketCommentDto> update(
            @PathVariable Long ticketId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateTicketCommentRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(ticketCommentService.updateComment(ticketId, commentId, request, currentUser));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long ticketId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        ticketCommentService.deleteComment(ticketId, commentId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
