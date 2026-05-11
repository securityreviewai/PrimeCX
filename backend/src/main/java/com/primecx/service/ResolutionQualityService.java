package com.primecx.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.primecx.model.Role;
import com.primecx.model.Ticket;
import com.primecx.model.TicketComment;

import lombok.extern.slf4j.Slf4j;

/**
 * Computes a 0–100 resolution quality score from reopen history, CSAT, and staff response signals.
 */
@Slf4j
@Service
public class ResolutionQualityService {

    private static final double MAX_REOPEN = 40.0;
    private static final double MAX_CSAT = 35.0;
    private static final double MAX_RESPONSE = 25.0;
    private static final double REOPEN_PENALTY = 12.0;
    private static final double NEUTRAL_CSAT = 17.5;

    /**
     * Mutates {@code ticket} with component scores and total. Caller persists.
     */
    public void assignQualityScores(Ticket ticket, List<TicketComment> commentsChronological) {
        int reopens = ticket.getReopenCount() != null ? ticket.getReopenCount() : 0;
        double reopenPts = Math.max(0.0, MAX_REOPEN - reopens * REOPEN_PENALTY);

        double csatPts = NEUTRAL_CSAT;
        Integer csat = ticket.getCsatRating();
        if (csat != null && csat >= 1 && csat <= 5) {
            csatPts = (csat - 1) / 4.0 * MAX_CSAT;
        }

        double responsePts = computeResponseQuality(ticket, commentsChronological);

        double total = round1(reopenPts + csatPts + responsePts);
        total = Math.min(100.0, Math.max(0.0, total));

        ticket.setResolutionQualityReopenPoints(round1(reopenPts));
        ticket.setResolutionQualityCsatPoints(round1(csatPts));
        ticket.setResolutionQualityResponsePoints(round1(responsePts));
        ticket.setResolutionQualityScore(total);

        log.debug(
                "Resolution quality ticket={} total={} reopen={} csat={} response={}",
                ticket.getId(),
                total,
                reopenPts,
                csatPts,
                responsePts
        );
    }

    private static double computeResponseQuality(Ticket ticket, List<TicketComment> comments) {
        LocalDateTime ticketStart = ticket.getCreatedAt();
        List<TicketComment> staffComments = comments.stream()
                .filter(c -> !c.isInternal())
                .filter(c -> c.getAuthor() != null && c.getAuthor().getRole() != Role.ROLE_USER)
                .toList();

        if (staffComments.isEmpty()) {
            return 0.0;
        }

        double firstResponse = 0.0;
        TicketComment firstStaff = staffComments.get(0);
        if (ticketStart != null && firstStaff.getCreatedAt() != null) {
            long hours = Duration.between(ticketStart, firstStaff.getCreatedAt()).toHours();
            if (hours <= 2) {
                firstResponse = 10.0;
            } else if (hours <= 8) {
                firstResponse = 7.0;
            } else if (hours <= 24) {
                firstResponse = 4.0;
            } else if (hours <= 72) {
                firstResponse = 2.0;
            }
        } else {
            firstResponse = 5.0;
        }

        int n = staffComments.size();
        double engagement = Math.min(10.0, n * 2.5);

        double totalLen = 0;
        for (TicketComment c : staffComments) {
            String b = c.getBody();
            totalLen += b != null ? b.length() : 0;
        }
        double avgLen = totalLen / n;
        double substance = Math.min(5.0, avgLen / 120.0 * 5.0);

        double sum = firstResponse + engagement + substance;
        return Math.min(MAX_RESPONSE, sum);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
