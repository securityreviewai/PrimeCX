package com.primecx.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.PagedTicketActivityResponse;
import com.primecx.model.User;
import com.primecx.service.TicketActivityService;
import com.primecx.service.TicketService;
import com.primecx.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/tickets/{ticketId:\\d+}/activity")
@RequiredArgsConstructor
public class TicketActivityController {

    private final TicketActivityService ticketActivityService;
    private final TicketService ticketService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<PagedTicketActivityResponse> listActivity(
            @PathVariable Long ticketId,
            @PageableDefault(size = 40, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User viewer = userService.getUserByOktaId(oidcUser.getSubject());
        ticketService.getTicketVisibleTo(ticketId, viewer);
        return ResponseEntity.ok(ticketActivityService.listForTicket(ticketId, pageable));
    }
}
