package com.primecx.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.CreateTicketMessageRequest;
import com.primecx.dto.TicketMessageDto;
import com.primecx.model.User;
import com.primecx.service.TicketMessageService;
import com.primecx.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/tickets/{ticketId}/messages")
@RequiredArgsConstructor
public class TicketMessageController {

    private final TicketMessageService ticketMessageService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<TicketMessageDto> createMessage(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateTicketMessageRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        TicketMessageDto dto = ticketMessageService.createMessage(ticketId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<TicketMessageDto>> getMessages(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(ticketMessageService.getMessages(ticketId, currentUser));
    }
}
