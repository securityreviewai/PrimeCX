package com.primecx.specification;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.primecx.model.Ticket;
import com.primecx.model.TicketPriority;
import com.primecx.model.TicketStatus;
import com.primecx.model.User;

public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> visibleToUser(User viewer) {
        return switch (viewer.getRole()) {
            case ROLE_SUPPORT_ADMIN, ROLE_SUPPORT_MANAGER -> (root, query, cb) -> cb.conjunction();
            case ROLE_SUPPORT_EXECUTIVE -> (root, query, cb) -> cb.equal(root.get("assignedTo").get("id"), viewer.getId());
            case ROLE_USER -> (root, query, cb) -> cb.equal(root.get("user").get("id"), viewer.getId());
        };
    }

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(TicketPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> unassignedClaimable() {
        return (root, query, cb) -> cb.and(
                cb.isNull(root.get("assignedTo")),
                cb.equal(root.get("status"), TicketStatus.OPEN));
    }

    /** Active tickets whose first-response SLA deadline has passed (calendar-time clock). */
    public static Specification<Ticket> slaOverdueActive() {
        LocalDateTime now = LocalDateTime.now();
        return (root, query, cb) -> cb.and(
                cb.or(
                        cb.equal(root.get("status"), TicketStatus.OPEN),
                        cb.equal(root.get("status"), TicketStatus.IN_PROGRESS)),
                cb.isNotNull(root.get("slaRespondBy")),
                cb.lessThan(root.get("slaRespondBy"), now));
    }

    public static Specification<Ticket> matchesSearch(String raw) {
        String q = raw == null ? "" : raw.strip();
        if (q.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        String literal = stripLikeMetacharacters(q);
        Specification<Ticket> textMatch;
        if (literal.isEmpty()) {
            textMatch = (root, query, cb) -> cb.disjunction();
        } else {
            String pattern = "%" + literal + "%";
            textMatch = (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        }

        if (q.chars().allMatch(Character::isDigit)) {
            try {
                long id = Long.parseLong(q);
                Specification<Ticket> idMatch = (root, query, cb) -> cb.equal(root.get("id"), id);
                return idMatch.or(textMatch);
            } catch (NumberFormatException ignored) {
                /* fall through to text-only */
            }
        }
        return textMatch;
    }

    private static String stripLikeMetacharacters(String value) {
        return value.replace("\\", "").replace("%", "").replace("_", "").trim().toLowerCase();
    }
}
