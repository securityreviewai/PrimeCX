package com.primecx.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.SlaComplianceSummaryDto;
import com.primecx.dto.TicketDto;
import com.primecx.model.Ticket;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketAnalyticsReportService {

    private final TicketService ticketService;
    private final SlaService slaService;

    @Transactional(readOnly = true)
    public SlaComplianceSummaryDto getSlaComplianceSummary() {
        List<Ticket> tickets = ticketService.getAllTickets();

        long responseMet = 0;
        long responseBreached = 0;
        long responsePending = 0;
        long resolveMet = 0;
        long resolveBreached = 0;
        long resolvePending = 0;

        for (Ticket ticket : tickets) {
            if (ticket.getFirstRespondedAt() == null) {
                if (slaService.isResponseBreached(ticket)) {
                    responseBreached++;
                } else {
                    responsePending++;
                }
            } else if (slaService.isResponseMet(ticket)) {
                responseMet++;
            } else {
                responseBreached++;
            }

            if (ticket.getStatus() == com.primecx.model.TicketStatus.RESOLVED
                    || ticket.getStatus() == com.primecx.model.TicketStatus.CLOSED) {
                if (slaService.isResolveMet(ticket)) {
                    resolveMet++;
                } else {
                    resolveBreached++;
                }
            } else if (slaService.isResolveBreached(ticket)) {
                resolveBreached++;
            } else {
                resolvePending++;
            }
        }

        long total = tickets.size();
        long responseCompleted = responseMet + responseBreached;
        long resolveCompleted = resolveMet + resolveBreached;

        double responseCompliance = responseCompleted == 0
                ? 100.0
                : (responseMet * 100.0) / responseCompleted;
        double resolveCompliance = resolveCompleted == 0
                ? 100.0
                : (resolveMet * 100.0) / resolveCompleted;

        return new SlaComplianceSummaryDto(
                total,
                responseMet,
                responseBreached,
                responsePending,
                resolveMet,
                resolveBreached,
                resolvePending,
                Math.round(responseCompliance * 10.0) / 10.0,
                Math.round(resolveCompliance * 10.0) / 10.0
        );
    }

    @Transactional(readOnly = true)
    public List<TicketDto> getBreachedTickets() {
        return ticketService.getAllTickets().stream()
                .filter(t -> slaService.isResponseBreached(t) || slaService.isResolveBreached(t))
                .map(ticketService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketDto> getAtRiskTickets() {
        return ticketService.getAllTickets().stream()
                .filter(t -> slaService.isResponseAtRisk(t) || slaService.isResolveAtRisk(t))
                .map(ticketService::toDto)
                .toList();
    }
}
