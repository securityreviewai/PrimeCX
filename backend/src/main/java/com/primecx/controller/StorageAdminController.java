package com.primecx.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.config.PrimecxStorageProperties;
import com.primecx.dto.AssignUserOrganizationRequest;
import com.primecx.dto.CreateOrganizationRequest;
import com.primecx.dto.OrganizationDto;
import com.primecx.dto.RetentionPolicyDto;
import com.primecx.dto.RecordingDto;
import com.primecx.dto.SetLegalHoldRequest;
import com.primecx.dto.UpsertRetentionPolicyRequest;
import com.primecx.dto.UserDto;
import com.primecx.model.RetentionPolicy;
import com.primecx.model.User;
import com.primecx.service.AuditLogService;
import com.primecx.service.OrganizationService;
import com.primecx.service.RecordingService;
import com.primecx.service.RetentionPolicyService;
import com.primecx.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/storage")
@PreAuthorize("hasRole('SUPPORT_ADMIN')")
@Validated
@RequiredArgsConstructor
public class StorageAdminController {

    private final OrganizationService organizationService;
    private final RetentionPolicyService retentionPolicyService;
    private final RecordingService recordingService;
    private final UserService userService;
    private final PrimecxStorageProperties storageProperties;
    private final AuditLogService auditLogService;

    @GetMapping("/defaults")
    public ResponseEntity<PrimecxStorageProperties> getDefaults() {
        return ResponseEntity.ok(storageProperties);
    }

    @GetMapping("/organizations")
    public ResponseEntity<List<OrganizationDto>> listOrganizations() {
        return ResponseEntity.ok(organizationService.listAll());
    }

    @PostMapping("/organizations")
    public ResponseEntity<OrganizationDto> createOrganization(@Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.ok(organizationService.create(request));
    }

    @GetMapping("/retention-policies")
    public ResponseEntity<List<RetentionPolicyDto>> listRetentionPolicies() {
        return ResponseEntity.ok(
                retentionPolicyService.listAll().stream().map(this::toPolicyDto).toList());
    }

    @PutMapping("/retention-policies")
    public ResponseEntity<RetentionPolicyDto> upsertRetentionPolicy(
            @Valid @RequestBody UpsertRetentionPolicyRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        var org = request.organizationId() == null
                ? null
                : organizationService.getById(request.organizationId());
        RetentionPolicy p = retentionPolicyService.upsert(
                org, request.s3Bucket(), request.retentionDays(), request.softDeleteGraceDays());
        auditLogService.appendRetentionPolicyUpserted(
                actor.getId(), p.getId(), p.getS3Bucket(),
                org != null ? org.getId() : null, p.getRetentionDays(), p.getSoftDeleteGraceDays());
        return ResponseEntity.ok(toPolicyDto(p));
    }

    @PutMapping("/users/{userId}/organization")
    public ResponseEntity<UserDto> assignUserOrganization(
            @PathVariable Long userId,
            @Valid @RequestBody AssignUserOrganizationRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        userService.getUserByOktaId(oidcUser.getSubject());
        User u = userService.assignOrganization(userId, request.organizationId());
        return ResponseEntity.ok(userService.toDto(u));
    }

    @PatchMapping("/recordings/{recordingId}/legal-hold")
    public ResponseEntity<RecordingDto> setLegalHold(
            @PathVariable Long recordingId,
            @Valid @RequestBody SetLegalHoldRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        var rec = recordingService.setLegalHold(recordingId, request.legalHold(), actor.getId());
        return ResponseEntity.ok(recordingService.toAdminDto(rec));
    }

    @PostMapping("/recordings/{recordingId}/soft-delete")
    public ResponseEntity<Void> softDelete(
            @PathVariable Long recordingId,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User actor = userService.getUserByOktaId(oidcUser.getSubject());
        recordingService.softDeleteRecording(recordingId, actor.getId());
        return ResponseEntity.noContent().build();
    }

    private RetentionPolicyDto toPolicyDto(RetentionPolicy p) {
        Long orgId = p.getOrganization() != null ? p.getOrganization().getId() : null;
        String orgName = p.getOrganization() != null ? p.getOrganization().getName() : null;
        return new RetentionPolicyDto(
                p.getId(), orgId, orgName, p.getS3Bucket(), p.getRetentionDays(), p.getSoftDeleteGraceDays());
    }
}
