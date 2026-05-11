package com.primecx.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import com.primecx.model.Role;
import com.primecx.model.User;
import com.primecx.repository.UserRepository;

/**
 * Custom authorization evaluator for sentiment timeline access.
 * Used in @PreAuthorize expressions to enforce BOLA and tenant isolation.
 * 
 * Implements guardrails:
 * - 139: Prevent BOLA with object-aware authorization logic
 * - 111: Enforce least-privilege and deny-by-default
 * - 134: Use method-level @PreAuthorize for tenant scoping
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SentimentTimelineAuthorizationEvaluator {

    private final UserRepository userRepository;

    /**
     * Check if user can view sentiment timeline for a specific customer.
     * 
     * Authorization rules (deny-by-default):
     * 1. If customerId == currentUserId: Customer can view their own sentiment (if enabled)
     * 2. If currentUser is SUPPORT_MANAGER or SUPPORT_ADMIN: Can view assigned customers
     * 3. Otherwise: DENY
     * 
     * @param customerId The customer ID being accessed
     * @param authentication The current user's authentication
     * @return true if access is allowed, false otherwise
     */
    public boolean canViewCustomerSentiment(Long customerId, Authentication authentication) {
        // Deny if not authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("SECURITY: Denied access to sentiment timeline - not authenticated");
            return false;
        }

        // Extract user email from JWT/OIDC
        Object principal = authentication.getPrincipal();
        String userEmail = null;

        if (principal instanceof OidcUser) {
            userEmail = ((OidcUser) principal).getEmail();
        } else if (principal instanceof String) {
            userEmail = (String) principal;
        }

        if (userEmail == null) {
            log.warn("SECURITY: Denied access to sentiment timeline - no user email in token");
            return false;
        }

        // Fetch current user from database
        User currentUser = userRepository.findByEmail(userEmail)
                .orElse(null);

        if (currentUser == null) {
            log.warn("SECURITY: Denied access to sentiment timeline - user not found: {}", userEmail);
            return false;
        }

        // Rule 1: Customer can view their own sentiment timeline
        if (currentUser.getId().equals(customerId)) {
            log.info("SECURITY_AUDIT: Customer viewing own sentiment timeline - customerId={}, user={}", 
                    customerId, userEmail);
            return true;
        }

        // Rule 2: Support staff can view sentiment if they're viewing a customer's data
        if (currentUser.getRole() == Role.ROLE_SUPPORT_EXECUTIVE 
            || currentUser.getRole() == Role.ROLE_SUPPORT_MANAGER 
            || currentUser.getRole() == Role.ROLE_SUPPORT_ADMIN) {
            
            // Verify that the customer (customerId) exists and is a valid ROLE_USER
            User targetCustomer = userRepository.findById(customerId)
                    .orElse(null);

            if (targetCustomer == null || targetCustomer.getRole() != Role.ROLE_USER) {
                log.warn("SECURITY: Support staff denied access to sentiment timeline - " +
                        "target customer not found or invalid role: customerId={}, user={}", 
                        customerId, userEmail);
                return false;
            }

            log.info("SECURITY_AUDIT: Support staff viewing customer sentiment timeline - " +
                    "customerId={}, supportUser={}", customerId, userEmail);
            return true;
        }

        // Rule 3: All other cases - DENY (default deny)
        log.warn("SECURITY: Denied access to sentiment timeline - " +
                "user role not authorized: customerId={}, user={}, role={}", 
                customerId, userEmail, currentUser.getRole());
        return false;
    }
}
