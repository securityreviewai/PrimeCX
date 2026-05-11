package com.primecx.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import jakarta.validation.Valid;

import com.primecx.dto.CreateTicketRequest;
import com.primecx.dto.TicketDto;
import com.primecx.dto.UpdateTicketRequest;
import com.primecx.model.Role;
import com.primecx.model.Ticket;
import com.primecx.model.TicketStatus;
import com.primecx.model.User;
import com.primecx.service.TicketService;
import com.primecx.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<TicketDto> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        Ticket ticket = ticketService.createTicket(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.toDto(ticket));
    }

    @GetMapping
    public ResponseEntity<List<TicketDto>> getTickets(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam(required = false) Boolean overdueOnly) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        List<Ticket> tickets;

        if (currentUser.getRole() == Role.ROLE_SUPPORT_ADMIN || currentUser.getRole() == Role.ROLE_SUPPORT_MANAGER) {
            tickets = ticketService.getAllTickets();
        } else if (currentUser.getRole() == Role.ROLE_SUPPORT_EXECUTIVE) {
            tickets = ticketService.getTicketsByAssignee(currentUser.getId());
        } else {
            tickets = ticketService.getTicketsByUser(currentUser.getId());
        }

        if (Boolean.TRUE.equals(overdueOnly)) {
            tickets = tickets.stream().filter(ticketService::isOverdue).toList();
        }

        List<TicketDto> dtos = tickets.stream().map(ticketService::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDto> getTicketById(
            @PathVariable Long id,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        Ticket ticket = ticketService.getTicketForViewer(id, currentUser);
        return ResponseEntity.ok(ticketService.toDto(ticket));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketDto> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        Ticket ticket = ticketService.updateTicket(id, request, currentUser.getId());
        return ResponseEntity.ok(ticketService.toDto(ticket));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TicketDto>> getTicketsByStatus(@PathVariable TicketStatus status) {
        List<TicketDto> dtos = ticketService.getTicketsByStatus(status).stream()
                .map(ticketService::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTicketsCsv(@AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getUserByOktaId(oidcUser.getSubject());
        List<Ticket> tickets;

        if (currentUser.getRole() == Role.ROLE_SUPPORT_ADMIN || currentUser.getRole() == Role.ROLE_SUPPORT_MANAGER) {
            tickets = ticketService.getAllTickets();
        } else if (currentUser.getRole() == Role.ROLE_SUPPORT_EXECUTIVE) {
            tickets = ticketService.getTicketsByAssignee(currentUser.getId());
        } else {
            tickets = ticketService.getTicketsByUser(currentUser.getId());
        }

        byte[] csvBytes = ticketService.generateCsv(tickets);

        log.info("CSV export by user {} (role={}), {} records", currentUser.getId(), currentUser.getRole(), tickets.size());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }
}
