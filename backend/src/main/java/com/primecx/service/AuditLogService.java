package com.primecx.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.primecx.dto.AuditLogEntryDto;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.AuditEventType;
import com.primecx.model.AuditLogEntry;
import com.primecx.model.Role;
import com.primecx.model.User;
import com.primecx.repository.AuditLogRepository;
import com.primecx.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<AuditLogEntryDto> findAllPaged(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDto);
    }

    @Transactional
    public void appendRoleChanged(Long actorUserId, Long targetUserId, Role oldRole, Role newRole) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));
        Map<String, String> details = new LinkedHashMap<>();
        details.put("oldRole", oldRole.name());
        details.put("newRole", newRole.name());
        persist(actor, AuditEventType.ROLE_CHANGED, targetUserId, null, null, null, details);
    }

    @Transactional
    public void appendUserDeactivated(Long actorUserId, Long targetUserId) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));
        persist(actor, AuditEventType.USER_DEACTIVATED, targetUserId, null, null, null, Map.of());
    }

    @Transactional
    public void appendUserReactivated(Long actorUserId, Long targetUserId) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));
        persist(actor, AuditEventType.USER_REACTIVATED, targetUserId, null, null, null, Map.of());
    }

    @Transactional
    public void appendUserProfileUpdated(Long actorUserId, Long targetUserId, Map<String, String> changedFields) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));
        persist(actor, AuditEventType.USER_PROFILE_UPDATED, targetUserId, null, null, null, changedFields);
    }

    @Transactional
    public void appendUserOrganizationAssigned(Long actorUserId, Long targetUserId, Long organizationId) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));
        Map<String, String> details = new LinkedHashMap<>();
        details.put("organizationId", String.valueOf(organizationId));
        persist(actor, AuditEventType.USER_ORGANIZATION_ASSIGNED, targetUserId, null, null, null, details);
    }

    @Transactional
    public void appendSessionEnded(Long actorUserId, Long sessionId, Long ticketId) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));
        persist(actor, AuditEventType.SESSION_ENDED, null, sessionId, null, ticketId, Map.of());
    }

    @Transactional
    public void appendRecordingConfirmed(Long actorUserId, Long recordingId, Long sessionId, String fileName) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));
        Map<String, String> details = new LinkedHashMap<>();
        if (fileName != null && !fileName.isBlank()) {
            details.put("fileName", fileName);
        }
        persist(actor, AuditEventType.RECORDING_CONFIRMED, null, sessionId, recordingId, null, details);
    }

    @Transactional
    public void appendRecordingSoftDeleted(Long actorUserId, Long recordingId, Long sessionId, String s3Key) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));
        Map<String, String> details = new LinkedHashMap<>();
        if (s3Key != null && !s3Key.isBlank()) {
            details.put("s3Key", s3Key);
        }
        persist(actor, AuditEventType.RECORDING_SOFT_DELETED, null, sessionId, recordingId, null, details);
    }

    @Transactional
    public void appendRecordingLegalHoldChanged(Long actorUserId, Long recordingId, Long sessionId, boolean hold) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));
        Map<String, String> details = Map.of("legalHold", Boolean.toString(hold));
        persist(actor, AuditEventType.RECORDING_LEGAL_HOLD_CHANGED, null, sessionId, recordingId, null, details);
    }

    @Transactional
    public void appendRetentionPolicyUpserted(
            Long actorUserId, Long policyId, String s3Bucket, Long organizationId, int retentionDays, int graceDays) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));
        Map<String, String> d = new LinkedHashMap<>();
        d.put("policyId", String.valueOf(policyId));
        d.put("s3Bucket", s3Bucket);
        if (organizationId != null) {
            d.put("organizationId", String.valueOf(organizationId));
        }
        d.put("retentionDays", String.valueOf(retentionDays));
        d.put("softDeleteGraceDays", String.valueOf(graceDays));
        persist(actor, AuditEventType.RETENTION_POLICY_UPSERTED, null, null, null, null, d);
    }

    private void persist(User actor, AuditEventType eventType, Long targetUserId,
            Long sessionId, Long recordingId, Long ticketId, Map<String, String> details) {
        AuditLogEntry entry = AuditLogEntry.builder()
                .actor(actor)
                .actorEmailSnapshot(actor.getEmail() != null ? actor.getEmail() : "")
                .eventType(eventType)
                .targetUserId(targetUserId)
                .sessionId(sessionId)
                .recordingId(recordingId)
                .ticketId(ticketId)
                .detailsJson(toJson(details))
                .build();
        auditLogRepository.save(entry);
    }

    private String toJson(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.warn("Audit details serialization failed: {}", e.getMessage());
            return "{}";
        }
    }

    private AuditLogEntryDto toDto(AuditLogEntry e) {
        return new AuditLogEntryDto(
                e.getId(),
                e.getCreatedAt(),
                e.getActor().getId(),
                e.getActorEmailSnapshot(),
                e.getEventType(),
                e.getTargetUserId(),
                e.getSessionId(),
                e.getRecordingId(),
                e.getTicketId(),
                e.getDetailsJson()
        );
    }
}
