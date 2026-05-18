package com.primecx.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.ConfirmTicketAttachmentRequest;
import com.primecx.dto.TicketAttachmentDto;
import com.primecx.dto.TicketAttachmentUploadUrlResponse;
import com.primecx.exception.ForbiddenException;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Role;
import com.primecx.model.Ticket;
import com.primecx.model.TicketActivityType;
import com.primecx.model.TicketAttachment;
import com.primecx.model.User;
import com.primecx.repository.TicketAttachmentRepository;
import com.primecx.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketAttachmentService {

    private static final long MAX_ATTACHMENT_BYTES = 25L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/webp",
            "application/pdf",
            "text/plain");

    private final TicketAttachmentRepository attachmentRepository;
    private final TicketService ticketService;
    private final TicketActivityService ticketActivityService;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    public TicketAttachmentUploadUrlResponse requestUploadUrl(Long ticketId, User u, String fileName, String contentType) {
        ticketService.getTicketVisibleTo(ticketId, u);
        validateFileNameAndContentType(fileName, contentType);

        String key = s3StorageService.generateTicketAttachmentKey(ticketId, fileName);
        String normalizedType = normalizeContentType(contentType);
        String uploadUrl = s3StorageService.generatePresignedUploadUrl(key, normalizedType);
        log.info("Issued attachment upload URL for ticket {} key {}", ticketId, key);
        return new TicketAttachmentUploadUrlResponse(uploadUrl, key, normalizedType);
    }

    @Transactional
    public TicketAttachmentDto confirm(Long ticketId, User u, ConfirmTicketAttachmentRequest req) {
        Ticket ticket = ticketService.getTicketVisibleTo(ticketId, u);
        validateAttachmentMeta(req.fileName(), req.contentType(), req.fileSize());

        String prefix = "ticket-attachments/" + ticketId + "/";
        if (!req.s3Key().startsWith(prefix)) {
            throw new IllegalArgumentException("Attachment key does not match this ticket.");
        }

        User author = userRepository.findById(u.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", u.getId()));

        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicket(ticket);
        attachment.setUploadedBy(author);
        attachment.setS3Key(req.s3Key());
        attachment.setS3Bucket(s3StorageService.getBucketName());
        attachment.setFileName(req.fileName());
        attachment.setFileSize(req.fileSize());
        attachment.setContentType(normalizeContentType(req.contentType()));

        TicketAttachment saved = attachmentRepository.save(attachment);
        ticketActivityService.record(ticketId, author, TicketActivityType.ATTACHMENT_ADDED,
                "Added attachment: " + req.fileName());
        log.info("Confirmed attachment {} on ticket {}", saved.getId(), ticketId);
        return toDto(saved, true);
    }

    @Transactional(readOnly = true)
    public List<TicketAttachmentDto> listForTicket(Long ticketId, User viewer) {
        ticketService.getTicketVisibleTo(ticketId, viewer);
        return attachmentRepository.findByTicket_IdOrderByUploadedAtDesc(ticketId).stream()
                .map(a -> toDto(a, true))
                .toList();
    }

    private TicketAttachmentDto toDto(TicketAttachment a, boolean includeDownload) {
        User uploader = a.getUploadedBy();
        String uploadedByName = uploader.getFirstName() + " " + uploader.getLastName();
        String downloadUrl = includeDownload ? s3StorageService.generatePresignedDownloadUrl(a.getS3Key()) : null;
        return new TicketAttachmentDto(
                a.getId(),
                a.getTicket().getId(),
                a.getFileName(),
                a.getContentType(),
                a.getFileSize(),
                uploadedByName.strip(),
                a.getUploadedAt(),
                downloadUrl);
    }

    private void validateFileNameAndContentType(String fileName, String contentType) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required.");
        }
        if (fileName.length() > 512) {
            throw new IllegalArgumentException("File name is too long.");
        }
        String ct = normalizeContentType(contentType);
        if (!ALLOWED_TYPES.contains(ct)) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed: PNG, JPEG, GIF, WebP, PDF, or plain text.");
        }
    }

    private void validateAttachmentMeta(String fileName, String contentType, long fileSize) {
        validateFileNameAndContentType(fileName, contentType);
        if (fileSize <= 0 || fileSize > MAX_ATTACHMENT_BYTES) {
            throw new IllegalArgumentException("Attachment must be between 1 byte and 25 MB.");
        }
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        int semi = contentType.indexOf(';');
        String base = semi >= 0 ? contentType.substring(0, semi) : contentType;
        return base.trim().toLowerCase();
    }

    /**
     * Deletes attachment metadata and S3 object (support admins only — enforced here and at controller).
     */
    @Transactional
    public void deleteAttachmentForTicket(Long ticketId, Long attachmentId, User actor) {
        if (actor.getRole() != Role.ROLE_SUPPORT_ADMIN) {
            throw new ForbiddenException("Only administrators can delete ticket attachments.");
        }
        TicketAttachment a = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketAttachment", attachmentId));
        if (!a.getTicket().getId().equals(ticketId)) {
            throw new IllegalArgumentException("Attachment does not belong to this ticket.");
        }
        ticketService.getTicketVisibleTo(ticketId, actor);
        ticketActivityService.record(ticketId, actor, TicketActivityType.ATTACHMENT_REMOVED,
                "Removed attachment: " + a.getFileName());
        s3StorageService.deleteObject(a.getS3Key());
        attachmentRepository.delete(a);
        log.info("Deleted ticket attachment {} on ticket {}", attachmentId, ticketId);
    }
}
