package com.primecx.service;

import org.springframework.stereotype.Component;

import com.primecx.model.Role;
import com.primecx.model.SupportSession;
import com.primecx.model.User;

/**
 * Centralized authorization policy for deciding who may view a {@link SupportSession}.
 *
 * Kept in one place so any new surface (deep links, exports, embeds, notifications)
 * re-authorizes against the same rule set instead of re-implementing role checks ad-hoc.
 * This is the enforcement point for BOLA/least-privilege on session resources.
 */
@Component
public class SessionAccessPolicy {

    public boolean canViewSession(User caller, SupportSession session) {
        if (caller == null || session == null || !caller.isActive()) {
            return false;
        }

        Role role = caller.getRole();
        if (role == Role.ROLE_SUPPORT_ADMIN || role == Role.ROLE_SUPPORT_MANAGER) {
            return true;
        }

        Long callerId = caller.getId();
        if (callerId == null) {
            return false;
        }

        Long executiveId = session.getSupportExecutive() != null ? session.getSupportExecutive().getId() : null;
        Long userId = session.getUser() != null ? session.getUser().getId() : null;

        return callerId.equals(executiveId) || callerId.equals(userId);
    }

    /**
     * Who is allowed to mint a shareable deep link for a session. Same ruleset as
     * viewing — a caller cannot create a capability they themselves don't hold.
     */
    public boolean canCreateShareLink(User caller, SupportSession session) {
        return canViewSession(caller, session);
    }
}
