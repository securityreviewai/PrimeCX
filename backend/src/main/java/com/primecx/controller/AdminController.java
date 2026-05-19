package com.primecx.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.DashboardStats;
import com.primecx.dto.ExecutiveWorkloadDto;
import com.primecx.dto.PagedAdminActivityFeedResponse;
import com.primecx.dto.RecordingDto;
import com.primecx.dto.ResolutionTimeSummaryDto;
import com.primecx.dto.SatisfactionSummaryDto;
import com.primecx.dto.TicketCategoryMixDto;
import com.primecx.dto.TicketVolumeBucketDto;
import com.primecx.dto.UserDto;
import com.primecx.service.DashboardService;
import com.primecx.service.RecordingService;
import com.primecx.service.SatisfactionReportService;
import com.primecx.service.SupportWorkloadReportService;
import com.primecx.service.TicketActivityService;
import com.primecx.service.TicketAnalyticsReportService;
import com.primecx.service.TicketVolumeReportService;
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
    private final TicketActivityService ticketActivityService;
    private final SupportWorkloadReportService supportWorkloadReportService;
    private final SatisfactionReportService satisfactionReportService;
    private final TicketVolumeReportService ticketVolumeReportService;
    private final TicketAnalyticsReportService ticketAnalyticsReportService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/activity/recent")
    public ResponseEntity<PagedAdminActivityFeedResponse> recentTicketActivity(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ticketActivityService.listRecentAcrossAllTickets(pageable));
    }

    @GetMapping("/reports/executive-workload")
    public ResponseEntity<List<ExecutiveWorkloadDto>> executiveWorkload() {
        return ResponseEntity.ok(supportWorkloadReportService.executiveWorkloadSnapshot());
    }

    @GetMapping("/reports/satisfaction")
    public ResponseEntity<SatisfactionSummaryDto> satisfactionSummary() {
        return ResponseEntity.ok(satisfactionReportService.buildSummary());
    }

    @GetMapping("/reports/ticket-volume")
    public ResponseEntity<List<TicketVolumeBucketDto>> ticketVolume(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ticketVolumeReportService.ticketsCreatedPerDay(days));
    }

    @GetMapping("/reports/tickets-by-category")
    public ResponseEntity<List<TicketCategoryMixDto>> ticketsByCategory(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ticketAnalyticsReportService.ticketsCreatedByCategoryMix(days));
    }

    @GetMapping("/reports/resolution-time")
    public ResponseEntity<ResolutionTimeSummaryDto> resolutionTime(
            @RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(ticketAnalyticsReportService.resolutionTimeSummary(days));
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
