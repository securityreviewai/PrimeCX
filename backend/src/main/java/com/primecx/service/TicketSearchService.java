package com.primecx.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.TicketDto;
import com.primecx.model.Ticket;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketSearchService {

    @PersistenceContext
    private EntityManager entityManager;

    private final TicketService ticketService;

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<TicketDto> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String sql = "SELECT * FROM tickets WHERE title ILIKE '%" + query + "%'"
                + " OR description ILIKE '%" + query + "%'"
                + " ORDER BY created_at DESC";

        List<Ticket> tickets = entityManager.createNativeQuery(sql, Ticket.class).getResultList();
        return tickets.stream().map(ticketService::toDto).toList();
    }
}
