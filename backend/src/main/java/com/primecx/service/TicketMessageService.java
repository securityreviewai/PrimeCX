package com.primecx.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateTicketMessageRequest;
import com.primecx.dto.TicketMessageDto;
import com.primecx.exception.ForbiddenException;
import com.primecx.model.Role;
import com.primecx.model.Ticket;
import com.primecx.model.TicketMessage;
import com.primecx.model.User;
import com.primecx.repository.TicketMessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketMessageService {

    private final TicketMessageRepository ticketMessageRepository;
    private final TicketService ticketService;
    private final SlaService slaService;

    @Transactional
    public TicketMessageDto createMessage(Long ticketId, CreateTicketMessageRequest request, User currentUser) {
        Ticket ticket = ticketService.getTicketVisibleToUser(ticketId, currentUser);

        if (request.internalNote() && !isStaff(currentUser)) {
            throw new ForbiddenException("Only support staff can create internal notes");
        }

        TicketMessage message = TicketMessage.builder()
                .ticket(ticket)
                .author(currentUser)
                .body(request.body().trim())
                .internalNote(request.internalNote())
                .build();

        TicketMessage saved = ticketMessageRepository.save(message);
        log.info("Message added to ticket {} by user {}", ticketId, currentUser.getId());

        if (isStaff(currentUser) && !request.internalNote()) {
            slaService.recordFirstResponse(ticket);
        }

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketMessageDto> getMessages(Long ticketId, User currentUser) {
        ticketService.getTicketVisibleToUser(ticketId, currentUser);

        List<TicketMessage> messages = isStaff(currentUser)
                ? ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                : ticketMessageRepository.findByTicketIdAndInternalNoteFalseOrderByCreatedAtAsc(ticketId);

        return messages.stream().map(this::toDto).toList();
    }

    public TicketMessageDto toDto(TicketMessage message) {
        String authorName = message.getAuthor().getFirstName() + " " + message.getAuthor().getLastName();
        return new TicketMessageDto(
                message.getId(),
                message.getTicket().getId(),
                message.getAuthor().getId(),
                authorName,
                message.getBody(),
                message.isInternalNote(),
                message.getCreatedAt()
        );
    }

    private boolean isStaff(User user) {
        Role role = user.getRole();
        return role == Role.ROLE_SUPPORT_EXECUTIVE
                || role == Role.ROLE_SUPPORT_ADMIN
                || role == Role.ROLE_SUPPORT_MANAGER;
    }
}
