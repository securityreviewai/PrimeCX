package com.primecx.service;

import java.util.List;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateTicketCommentRequest;
import com.primecx.dto.TicketCommentDto;
import com.primecx.dto.UpdateTicketCommentRequest;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Role;
import com.primecx.model.Ticket;
import com.primecx.model.TicketComment;
import com.primecx.model.User;
import com.primecx.repository.TicketCommentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCommentService {

    private final TicketCommentRepository ticketCommentRepository;
    private final TicketService ticketService;

    @Transactional(readOnly = true)
    public List<TicketCommentDto> listComments(Long ticketId, User currentUser) {
        Ticket ticket = ticketService.getTicketById(ticketId);
        ticketService.requireTicketAccess(currentUser, ticket);

        boolean staff = isStaff(currentUser);
        return ticketCommentRepository.findVisibleForTicket(ticketId, staff).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public TicketCommentDto createComment(Long ticketId, CreateTicketCommentRequest request, User author) {
        Ticket ticket = ticketService.getTicketById(ticketId);
        ticketService.requireTicketAccess(author, ticket);

        boolean wantInternal = Boolean.TRUE.equals(request.internal());
        if (wantInternal && !isStaff(author)) {
            throw new AccessDeniedException("Only support staff may create internal notes");
        }

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .author(author)
                .body(request.body().trim())
                .internal(wantInternal && isStaff(author))
                .build();

        TicketComment saved = ticketCommentRepository.save(comment);
        ticketService.recordTicketLastTouch(ticket, author);
        log.info("Ticket comment created id={} ticketId={} authorId={} internal={}",
                saved.getId(), ticketId, author.getId(), saved.isInternal());
        return toDto(saved);
    }

    @Transactional
    public TicketCommentDto updateComment(Long ticketId, Long commentId, UpdateTicketCommentRequest request, User editor) {
        TicketComment comment = ticketCommentRepository.findByIdAndTicket_Id(commentId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketComment", commentId));
        Ticket ticket = comment.getTicket();
        ticketService.requireTicketAccess(editor, ticket);
        requireCanEditComment(editor, comment);

        comment.setBody(request.body().trim());
        if (request.internal() != null) {
            if (!isStaff(editor)) {
                throw new AccessDeniedException("Only support staff may change internal visibility");
            }
            comment.setInternal(request.internal());
        }

        TicketComment saved = ticketCommentRepository.save(comment);
        ticketService.recordTicketLastTouch(ticket, editor);
        log.info("Ticket comment updated id={} ticketId={} editorId={}", commentId, ticketId, editor.getId());
        return toDto(saved);
    }

    @Transactional
    public void deleteComment(Long ticketId, Long commentId, User actor) {
        TicketComment comment = ticketCommentRepository.findByIdAndTicket_Id(commentId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketComment", commentId));
        Ticket ticket = comment.getTicket();
        ticketService.requireTicketAccess(actor, ticket);
        requireCanEditComment(actor, comment);

        ticketService.recordTicketLastTouch(ticket, actor);
        ticketCommentRepository.delete(comment);
        log.info("Ticket comment deleted id={} ticketId={} actorId={}", commentId, ticketId, actor.getId());
    }

    private static boolean isStaff(User u) {
        return u.getRole() != Role.ROLE_USER;
    }

    private static void requireCanEditComment(User actor, TicketComment comment) {
        Role r = actor.getRole();
        if (r == Role.ROLE_SUPPORT_ADMIN || r == Role.ROLE_SUPPORT_MANAGER) {
            return;
        }
        if (Objects.equals(comment.getAuthor().getId(), actor.getId())) {
            return;
        }
        throw new AccessDeniedException("Not authorized to modify this comment");
    }

    private TicketCommentDto toDto(TicketComment c) {
        User a = c.getAuthor();
        String display = ((a.getFirstName() != null ? a.getFirstName() : "") + " "
                + (a.getLastName() != null ? a.getLastName() : "")).trim();
        if (display.isEmpty()) {
            display = a.getEmail() != null ? a.getEmail() : ("user-" + a.getId());
        }
        return new TicketCommentDto(
                c.getId(),
                c.getTicket().getId(),
                a.getId(),
                display,
                a.getRole().name(),
                c.getBody(),
                c.isInternal(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
