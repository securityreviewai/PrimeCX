package com.primecx.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.CreateTicketMessageRequest;
import com.primecx.dto.CreateTicketRequest;
import com.primecx.dto.PagedTicketsResponse;
import com.primecx.dto.TicketDto;
import com.primecx.dto.TicketMessageDto;
import com.primecx.dto.TicketStatsResponse;
import com.primecx.dto.UpdateTicketRequest;
import com.primecx.model.Role;
import com.primecx.model.Ticket;
import com.primecx.model.TicketPriority;
import com.primecx.model.TicketStatus;
import com.primecx.model.User;
import com.primecx.service.TicketMessageService;
import com.primecx.service.TicketService;
import com.primecx.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketMessageService ticketMessageService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<TicketDto> createTicket(
            @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        Ticket ticket = ticketService.createTicket(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.toDto(ticket));
    }

    @GetMapping
    public ResponseEntity<List<TicketDto>> getTickets(@AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        List<Ticket> tickets;

        if (currentUser.getRole() == Role.ROLE_SUPPORT_ADMIN || currentUser.getRole() == Role.ROLE_SUPPORT_MANAGER) {
            tickets = ticketService.getAllTickets();
        } else if (currentUser.getRole() == Role.ROLE_SUPPORT_EXECUTIVE) {
            tickets = ticketService.getTicketsByAssignee(currentUser.getId());
        } else {
            tickets = ticketService.getTicketsByUser(currentUser.getId());
        }

        List<TicketDto> dtos = tickets.stream().map(ticketService::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/search")
    public ResponseEntity<PagedTicketsResponse> searchTickets(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false, name = "q") String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(ticketService.searchTickets(currentUser, status, priority, q, pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<TicketStatsResponse> ticketStats(@AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(ticketService.getTicketStats(currentUser));
    }

    @GetMapping("/{id:\\d+}/messages")
    public ResponseEntity<List<TicketMessageDto>> listTicketMessages(
            @PathVariable Long id,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(ticketMessageService.listMessages(id, currentUser));
    }

    @PostMapping("/{id:\\d+}/messages")
    public ResponseEntity<TicketMessageDto> addTicketMessage(
            @PathVariable Long id,
            @Valid @RequestBody CreateTicketMessageRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        TicketMessageDto created = ticketMessageService.addMessage(id, currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<TicketDto> getTicketById(
            @PathVariable Long id,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(ticketService.toDto(ticketService.getTicketVisibleTo(id, currentUser)));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<TicketDto> updateTicket(
            @PathVariable Long id,
            @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        Ticket ticket = ticketService.updateTicket(id, request, currentUser);
        return ResponseEntity.ok(ticketService.toDto(ticket));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<List<TicketDto>> getTicketsByStatus(@PathVariable TicketStatus status) {
        List<TicketDto> dtos = ticketService.getTicketsByStatus(status).stream()
                .map(ticketService::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }
}
