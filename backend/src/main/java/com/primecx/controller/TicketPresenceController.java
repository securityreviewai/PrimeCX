package com.primecx.controller;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.primecx.model.User;
import com.primecx.service.TicketPresenceService;
import com.primecx.service.TicketService;
import com.primecx.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketPresenceController {

    private final TicketPresenceService ticketPresenceService;
    private final TicketService ticketService;
    private final UserService userService;

    @GetMapping(value = "/{id}/presence/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTicketPresence(
            @PathVariable Long id,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User user = userService.getUserByOktaId(oidcUser.getSubject());
        ticketService.getTicketVisibleToUser(id, user);
        return ticketPresenceService.subscribe(id, user);
    }
}
