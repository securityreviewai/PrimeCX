package com.primecx.dto;

public record DashboardStats(
        long totalTickets,
        long openTickets,
        long criticalOpenTickets,
        long openEscalatedTickets,
        long activeSessions,
        long totalRecordings,
        long totalUsers
) {}
