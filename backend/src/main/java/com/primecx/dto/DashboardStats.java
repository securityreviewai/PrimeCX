package com.primecx.dto;

public record DashboardStats(
        long totalTickets,
        long openTickets,
        long overdueTickets,
        long activeSessions,
        long totalRecordings,
        long totalUsers
) {}
