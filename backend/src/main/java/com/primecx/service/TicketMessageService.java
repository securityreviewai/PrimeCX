package com.primecx.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateTicketMessageRequest;
import com.primecx.dto.TicketMessageDto;
import com.primecx.model.Ticket;
import com.primecx.model.TicketActivityType;
import com.primecx.model.TicketMessage;
import com.primecx.model.User;
import com.primecx.repository.TicketMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketMessageService {

    private final TicketMessageRepository ticketMessageRepository;
    private final TicketService ticketService;
    private final TicketActivityService ticketActivityService;

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
        TicketMessage saved = ticketMessageRepository.save(message);
        ticketActivityService.record(ticketId, author, TicketActivityType.MESSAGE_POSTED,
                messagePreview(saved.getBody()));
        return toDto(saved);
    }

    private static String messagePreview(String body) {
        if (body == null || body.isEmpty()) {
            return "Message posted";
        }
        String oneLine = body.replace('\n', ' ').strip();
        int max = 160;
        if (oneLine.length() <= max) {
            return oneLine;
        }
        return oneLine.substring(0, max - 1) + "…";
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
