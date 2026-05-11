package com.primecx.service;

import org.springframework.stereotype.Service;

import com.primecx.dto.ResolutionQualityOverviewDto;
import com.primecx.model.TicketStatus;
import com.primecx.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResolutionQualityOverviewService {

    private final TicketRepository ticketRepository;

    public ResolutionQualityOverviewDto buildOverview() {
        TicketStatus closed = TicketStatus.CLOSED;
        return new ResolutionQualityOverviewDto(
                ticketRepository.averageResolutionQualityScore(closed).orElse(null),
                ticketRepository.countWithResolutionQuality(closed),
                ticketRepository.countByStatus(closed),
                ticketRepository.averageReopenAmongScored(closed).orElse(null)
        );
    }
}
