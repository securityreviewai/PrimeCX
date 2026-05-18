package com.primecx.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateTicketMessageRequest;
import com.primecx.dto.TicketMessageDto;
import com.primecx.model.Ticket;
import com.primecx.model.TicketMessage;
import com.primecx.model.User;
import com.primecx.repository.TicketMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketMessageService {

    private final TicketMessageRepository ticketMessageRepository;
    private final TicketService ticketService;

    @Transactional(readOnly = true)
    public List<TicketMessageDto> listMessages(Long ticketId, User viewer) {
        ticketService.getTicketVisibleTo(ticketId, viewer);
        return ticketMessageRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public TicketMessageDto addMessage(Long ticketId, User author, CreateTicketMessageRequest request) {
        Ticket ticket = ticketService.getTicketVisibleTo(ticketId, author);
        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setAuthor(author);
        message.setBody(request.body().strip());
        return toDto(ticketMessageRepository.save(message));
    }

    private TicketMessageDto toDto(TicketMessage m) {
        User author = m.getAuthor();
        String authorName = author.getFirstName() + " " + author.getLastName();
        return new TicketMessageDto(
                m.getId(),
                m.getTicket().getId(),
                m.getBody(),
                m.getCreatedAt(),
                author.getId(),
                authorName.strip());
    }
}
