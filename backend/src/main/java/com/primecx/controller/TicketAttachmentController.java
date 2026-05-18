package com.primecx.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.ConfirmTicketAttachmentRequest;
import com.primecx.dto.CreateTicketAttachmentUploadRequest;
import com.primecx.dto.TicketAttachmentDto;
import com.primecx.dto.TicketAttachmentUploadUrlResponse;
import com.primecx.model.User;
import com.primecx.service.TicketAttachmentService;
import com.primecx.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/tickets/{ticketId:\\d+}/attachments")
@RequiredArgsConstructor
public class TicketAttachmentController {

    private final TicketAttachmentService attachmentService;
    private final UserService userService;

    @PostMapping("/upload-url")
    public ResponseEntity<TicketAttachmentUploadUrlResponse> requestUploadUrl(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateTicketAttachmentUploadRequest body,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User u = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(attachmentService.requestUploadUrl(ticketId, u, body.fileName(), body.contentType()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<TicketAttachmentDto> confirmUpload(
            @PathVariable Long ticketId,
            @Valid @RequestBody ConfirmTicketAttachmentRequest body,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User u = userService.getUserByOktaId(oidcUser.getSubject());
        TicketAttachmentDto dto = attachmentService.confirm(ticketId, u, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<TicketAttachmentDto>> listAttachments(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User u = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(attachmentService.listForTicket(ticketId, u));
    }

    @DeleteMapping("/{attachmentId:\\d+}")
    @PreAuthorize("hasRole('SUPPORT_ADMIN')")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User u = userService.getUserByOktaId(oidcUser.getSubject());
        attachmentService.deleteAttachmentForTicket(ticketId, attachmentId, u);
        return ResponseEntity.noContent().build();
    }
}
