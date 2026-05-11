package com.primecx.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.regex.Pattern;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Role;
import com.primecx.model.SessionShareLink;
import com.primecx.model.SupportSession;
import com.primecx.model.User;
import com.primecx.repository.SessionShareLinkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mint, resolve, and revoke shareable deep-links to support sessions.
 *
 * Design constraints enforced here:
 *  - Tokens are 128-bit SecureRandom, URL-safe base64, strictly validated on resolve.
 *  - A token is a lookup handle only. Callers MUST still be authenticated and MUST
 *    independently pass {@link SessionAccessPolicy} to read the underlying session.
 *  - Expiry is mandatory ({@link #DEFAULT_TTL}); revocation is supported.
 *  - Resolve failures (malformed, missing, expired, revoked, unauthorized) all return
 *    a generic not-found to the caller to avoid leaking token existence.
 *  - Raw token values are never logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionShareLinkService {

    static final Duration DEFAULT_TTL = Duration.ofDays(7);

    /** URL-safe base64 of 16 bytes, no padding, length 22. */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{22}$");
    private static final int TOKEN_ENTROPY_BYTES = 16;

    private static final SecureRandom RNG = new SecureRandom();

    private final SessionShareLinkRepository shareRepository;
    private final SupportSessionService supportSessionService;
    private final SessionAccessPolicy accessPolicy;

    @Transactional
    public SessionShareLink createForSession(Long sessionId, User caller) {
        SupportSession session = supportSessionService.getSessionById(sessionId);

        if (!accessPolicy.canCreateShareLink(caller, session)) {
            log.warn("share-link create denied sessionId={} callerId={}",
                    sessionId, caller.getId());
            throw new AccessDeniedException("Forbidden");
        }

        SessionShareLink link = new SessionShareLink();
        link.setToken(generateToken());
        link.setSession(session);
        link.setCreatedBy(caller);
        LocalDateTime now = LocalDateTime.now();
        link.setCreatedAt(now);
        link.setExpiresAt(now.plus(DEFAULT_TTL));

        SessionShareLink saved = shareRepository.save(link);
        log.info("share-link created id={} sessionId={} createdById={} expiresAt={}",
                saved.getId(), sessionId, caller.getId(), saved.getExpiresAt());
        return saved;
    }

    @Transactional(readOnly = true)
    public SessionShareLink resolve(String token, User caller) {
        if (token == null || !TOKEN_PATTERN.matcher(token).matches()) {
            log.warn("share-link resolve rejected: malformed token (callerId={})",
                    caller != null ? caller.getId() : null);
            throw genericNotFound();
        }

        SessionShareLink link = shareRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("share-link resolve miss callerId={}",
                            caller != null ? caller.getId() : null);
                    return genericNotFound();
                });

        LocalDateTime now = LocalDateTime.now();
        if (link.getRevokedAt() != null || link.getExpiresAt().isBefore(now)) {
            log.warn("share-link resolve rejected: expired_or_revoked id={} callerId={}",
                    link.getId(), caller != null ? caller.getId() : null);
            throw genericNotFound();
        }

        SupportSession session = link.getSession();
        if (!accessPolicy.canViewSession(caller, session)) {
            log.warn("share-link resolve denied id={} sessionId={} callerId={}",
                    link.getId(), session.getId(), caller != null ? caller.getId() : null);
            throw genericNotFound();
        }

        log.info("share-link resolved id={} sessionId={} callerId={}",
                link.getId(), session.getId(), caller.getId());
        return link;
    }

    @Transactional
    public void revoke(Long linkId, User caller) {
        SessionShareLink link = shareRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionShareLink", linkId));

        if (!canRevoke(caller, link)) {
            log.warn("share-link revoke denied id={} callerId={}", linkId, caller.getId());
            throw new AccessDeniedException("Forbidden");
        }

        if (link.getRevokedAt() == null) {
            link.setRevokedAt(LocalDateTime.now());
            shareRepository.save(link);
        }
        log.info("share-link revoked id={} byId={}", linkId, caller.getId());
    }

    private boolean canRevoke(User caller, SessionShareLink link) {
        if (caller == null || !caller.isActive()) {
            return false;
        }
        Role role = caller.getRole();
        if (role == Role.ROLE_SUPPORT_ADMIN || role == Role.ROLE_SUPPORT_MANAGER) {
            return true;
        }
        Long createdById = link.getCreatedBy() != null ? link.getCreatedBy().getId() : null;
        return createdById != null && createdById.equals(caller.getId());
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_ENTROPY_BYTES];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static ResourceNotFoundException genericNotFound() {
        // Intentionally uniform message for malformed / missing / expired / unauthorized
        // to avoid turning the endpoint into an enumeration oracle.
        return new ResourceNotFoundException("Share link", "not-found");
    }
}
