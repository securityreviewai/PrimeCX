package com.primecx.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.CreateShareLinkResponse;
import com.primecx.dto.ResolvedShareLinkDto;
import com.primecx.model.SessionShareLink;
import com.primecx.model.SupportSession;
import com.primecx.model.User;
import com.primecx.service.SessionShareLinkService;
import com.primecx.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Shareable deep links for support sessions.
 *
 * All routes sit under {@code /api/sessions/**} which is covered by the global
 * {@code authenticated()} matcher in {@code SecurityConfig}. Authorization is
 * re-evaluated in the service layer via {@link com.primecx.service.SessionAccessPolicy}.
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionShareLinkController {

    private final SessionShareLinkService shareLinkService;
    private final UserService userService;

    @PostMapping("/{id}/share-link")
    public ResponseEntity<CreateShareLinkResponse> create(
            @PathVariable Long id,
            @AuthenticationPrincipal OidcUser principal) {
        User caller = userService.getUserByOktaId(principal.getSubject());
        SessionShareLink link = shareLinkService.createForSession(id, caller);
        CreateShareLinkResponse body = new CreateShareLinkResponse(
                link.getId(),
                link.getSession().getId(),
                link.getToken(),
                "/s/" + link.getToken(),
                link.getExpiresAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/shared/{token}")
    public ResponseEntity<ResolvedShareLinkDto> resolve(
            @PathVariable String token,
            @AuthenticationPrincipal OidcUser principal) {
        User caller = userService.getUserByOktaId(principal.getSubject());
        SessionShareLink link = shareLinkService.resolve(token, caller);
        SupportSession session = link.getSession();
        ResolvedShareLinkDto body = new ResolvedShareLinkDto(
                session.getId(),
                session.getTicket() != null ? session.getTicket().getId() : null,
                session.getTicket() != null ? session.getTicket().getTitle() : null,
                session.getStatus(),
                session.getStartTime(),
                session.getEndTime(),
                link.getExpiresAt()
        );
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/share-links/{linkId}")
    public ResponseEntity<Void> revoke(
            @PathVariable Long linkId,
            @AuthenticationPrincipal OidcUser principal) {
        User caller = userService.getUserByOktaId(principal.getSubject());
        shareLinkService.revoke(linkId, caller);
        return ResponseEntity.noContent().build();
    }
}
