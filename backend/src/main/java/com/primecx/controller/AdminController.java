package com.primecx.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.DashboardStats;
import com.primecx.dto.RecordingDto;
import com.primecx.dto.UserDto;
import com.primecx.service.DashboardService;
import com.primecx.service.RecordingService;
import com.primecx.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    private final UserService userService;
    private final RecordingService recordingService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers().stream()
                .map(userService::toDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/recordings")
    public ResponseEntity<List<RecordingDto>> getRecordings(
            @RequestParam(required = false) Long executiveId) {
        List<RecordingDto> dtos;
        if (executiveId != null) {
            dtos = recordingService.getRecordingsByExecutive(executiveId).stream()
                    .map(r -> recordingService.toDto(r, false))
                    .toList();
        } else {
            dtos = recordingService.getAllRecordings().stream()
                    .map(r -> recordingService.toDto(r, false))
                    .toList();
        }
        return ResponseEntity.ok(dtos);
    }
}
