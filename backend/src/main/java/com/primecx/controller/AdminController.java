package com.primecx.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.AuditLogEntryDto;
import com.primecx.dto.RecordingDto;
import com.primecx.dto.UserDto;
import com.primecx.service.AuditLogService;
import com.primecx.service.RecordingService;
import com.primecx.service.UserService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
@Validated
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final RecordingService recordingService;
    private final AuditLogService auditLogService;

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUserDtos());
    }

    @GetMapping("/recordings")
    public ResponseEntity<List<RecordingDto>> getRecordings(
            @RequestParam(required = false) Long executiveId) {
        List<RecordingDto> dtos;
        if (executiveId != null) {
            dtos = recordingService.listRecordingAdminDtosByExecutive(executiveId);
        } else {
            dtos = recordingService.listAllRecordingAdminDtos();
        }
        return ResponseEntity.ok(dtos);
    }

    /**
     * Append-only audit trail: role changes, session ends, recording confirmations. Admin-only (see {@code SecurityFilterChain}).
     */
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('SUPPORT_ADMIN')")
    public ResponseEntity<Page<AuditLogEntryDto>> listAuditLogs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(auditLogService.findAllPaged(pageable));
    }
}
