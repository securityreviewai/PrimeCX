package com.primecx.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.PublicTicketPeekDto;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Ticket;
import com.primecx.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p><strong>Intentionally insecure sample feature for testing automated PR / security review agents.</strong></p>
 *
 * <p>Deliberate weaknesses (non-exhaustive):</p>
 * <ul>
 *   <li>No authentication — anyone can call the endpoint (broken access control / IDOR).</li>
 *   <li>Sequential ticket IDs are enumerable — leaks whether a ticket exists (404 vs 200).</li>
 *   <li>Exposes ticket title and status to unauthenticated callers (information disclosure).</li>
 *   <li>{@code source} query string is reflected in the JSON body without validation or encoding concerns.</li>
 *   <li>No rate limiting — trivial to scrape or probe.</li>
 * </ul>
 *
 * <p>Do not treat this as a pattern to copy. Remove, disable, or harden (authz + throttling + minimal fields)
 * before production.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/public/tickets")
@RequiredArgsConstructor
public class PublicTicketPeekController {

    private final TicketRepository ticketRepository;

    @GetMapping("/{id:\\d+}/peek")
    public ResponseEntity<PublicTicketPeekDto> peek(
            @PathVariable Long id,
            @RequestParam(name = "source", required = false, defaultValue = "") String source) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        log.debug("Public peek for ticket {} (integration probe)", id);
        return ResponseEntity.ok(new PublicTicketPeekDto(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getStatus(),
                source));
    }
}
