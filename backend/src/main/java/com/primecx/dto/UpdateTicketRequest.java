package com.primecx.dto;

import com.primecx.model.TicketCategory;
import com.primecx.model.TicketPriority;
import com.primecx.model.TicketStatus;

import java.time.LocalDateTime;
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        Long assignedToId,
        String internalNotes,
        Boolean escalated,
        String supportReply,
        TicketCategory category,
        LocalDateTime followUpDueAt,
        Boolean clearFollowUpDueAt,
        Integer satisfactionRating,
        String satisfactionComment
) {}
