package com.primecx.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.PublicTicketPeekDto;
import com.primecx.model.Ticket;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketPeekService {

    private final TicketService ticketService;

    @Transactional(readOnly = true)
    public PublicTicketPeekDto peek(Long ticketId) {
        Ticket ticket = ticketService.getTicketById(ticketId);
        String customerName = ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName();

        return new PublicTicketPeekDto(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                customerName,
                ticket.getUser().getEmail(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
