package com.primecx.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.SatisfactionSummaryDto;
import com.primecx.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SatisfactionReportService {

    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public SatisfactionSummaryDto buildSummary() {
        long withRating = ticketRepository.countByCustomerRatingIsNotNull();
        Double avg = ticketRepository.averageCustomerRating();
        return new SatisfactionSummaryDto(
                withRating,
                avg,
                ticketRepository.countByCustomerRating(1),
                ticketRepository.countByCustomerRating(2),
                ticketRepository.countByCustomerRating(3),
                ticketRepository.countByCustomerRating(4),
                ticketRepository.countByCustomerRating(5),
                ticketRepository.countTicketsWithNonEmptyFeedback());
    }
}
