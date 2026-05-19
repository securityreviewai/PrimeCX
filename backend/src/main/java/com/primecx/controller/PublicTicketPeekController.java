package com.primecx.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.PublicTicketPeekDto;
import com.primecx.service.TicketPeekService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/tickets")
@RequiredArgsConstructor
public class PublicTicketPeekController {

    private final TicketPeekService ticketPeekService;

    @GetMapping("/{id}")
    public ResponseEntity<PublicTicketPeekDto> peekTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketPeekService.peek(id));
    }
}
