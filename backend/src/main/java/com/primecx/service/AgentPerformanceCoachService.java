package com.primecx.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.primecx.dto.AgentCoachMetricsDto;
import com.primecx.dto.AgentCoachTipDto;
import com.primecx.dto.AgentPerformanceCoachDto;
import com.primecx.model.Role;
import com.primecx.model.Ticket;
import com.primecx.model.TicketChangeLog;
import com.primecx.model.TicketComment;
import com.primecx.model.TicketStatus;
import com.primecx.repository.TicketChangeLogRepository;
import com.primecx.repository.TicketCommentRepository;
import com.primecx.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

/**
 * Derives non-identifying coaching tips from the current user's assigned ticket history
 * (stage durations, customer waiting on replies, lightweight public-reply tone checks).
 */
@Service
@RequiredArgsConstructor
public class AgentPerformanceCoachService {

    private static final int LOOKBACK_DAYS = 90;
    private static final int MAX_TICKETS_ANALYZED = 80;
    private static final double PICKUP_WARN_HOURS = 24.0;
    private static final double PICKUP_EXAMPLE_HOURS = 48.0;
    private static final double IN_PROGRESS_WARN_HOURS = 72.0;
    private static final double FOLLOWUP_WARN_HOURS = 18.0;
    private static final int MAX_EXAMPLE_IDS = 5;

    private static final List<Pattern> TONE_PATTERNS = List.of(
            Pattern.compile("\\byou should have\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bobviously\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bas i (already )?said\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bnot our (fault|problem)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcalm down\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\byour fault\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final Set<TicketStatus> ACTIVE = EnumSet.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
    private static final Set<TicketStatus> CLOSED = EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final TicketRepository ticketRepository;
    private final TicketChangeLogRepository ticketChangeLogRepository;
    private final TicketCommentRepository ticketCommentRepository;

    public AgentPerformanceCoachDto buildCoachInsights(Long userId, Role role) {
        if (role == Role.ROLE_USER) {
            return new AgentPerformanceCoachDto(List.of(), new AgentCoachMetricsDto(null, null, 0, 0, 0));
        }

        LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);
        List<Ticket> coachTickets = ticketRepository.findCoachTicketsForAssignee(userId, since);
        coachTickets.sort(Comparator.comparing(Ticket::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
        if (coachTickets.size() > MAX_TICKETS_ANALYZED) {
            coachTickets = new ArrayList<>(coachTickets.subList(0, MAX_TICKETS_ANALYZED));
        }

        if (coachTickets.isEmpty()) {
            List<AgentCoachTipDto> emptyQueueTips = List.of(new AgentCoachTipDto(
                    "SLOW_STAGE",
                    "info",
                    "No recent assigned tickets in the coaching window",
                    "When you have tickets assigned to you, this panel will surface timing, follow-up, and tone patterns from your queue.",
                    List.of()
            ));
            return new AgentPerformanceCoachDto(emptyQueueTips, new AgentCoachMetricsDto(null, null, 0, 0, 0));
        }

        List<Long> ticketIds = coachTickets.stream().map(Ticket::getId).toList();
        List<TicketChangeLog> statusLogs = ticketChangeLogRepository.findStatusChangesForTickets(ticketIds);
        Map<Long, List<TicketChangeLog>> logsByTicket = new HashMap<>();
        for (TicketChangeLog log : statusLogs) {
            logsByTicket.computeIfAbsent(log.getTicket().getId(), k -> new ArrayList<>()).add(log);
        }
        for (List<TicketChangeLog> logs : logsByTicket.values()) {
            logs.sort(Comparator.comparing(TicketChangeLog::getChangedAt));
        }

        List<Double> pickupSamples = new ArrayList<>();
        List<Double> inProgressSamples = new ArrayList<>();
        List<Long> slowPickupExamples = new ArrayList<>();
        List<Long> slowProgressExamples = new ArrayList<>();
        int closedAnalyzed = 0;

        for (Ticket t : coachTickets) {
            if (!CLOSED.contains(t.getStatus())) {
                continue;
            }
            StageDurations d = computeStageDurations(t, logsByTicket.getOrDefault(t.getId(), List.of()));
            if (d == null) {
                continue;
            }
            closedAnalyzed++;
            if (d.pickupHours != null) {
                pickupSamples.add(d.pickupHours);
                if (d.pickupHours >= PICKUP_EXAMPLE_HOURS && slowPickupExamples.size() < MAX_EXAMPLE_IDS) {
                    slowPickupExamples.add(t.getId());
                }
            }
            if (d.inProgressHours != null) {
                inProgressSamples.add(d.inProgressHours);
                if (d.inProgressHours >= IN_PROGRESS_WARN_HOURS && slowProgressExamples.size() < MAX_EXAMPLE_IDS) {
                    slowProgressExamples.add(t.getId());
                }
            }
        }

        Double avgPickup = average(pickupSamples);
        Double avgInProgress = average(inProgressSamples);

        List<Ticket> activeAssigned = ticketRepository.findByAssignedToIdAndStatusIn(userId, ACTIVE);
        List<Long> activeIds = activeAssigned.stream().map(Ticket::getId).toList();
        Map<Long, List<TicketComment>> commentsByTicket = new HashMap<>();
        if (!activeIds.isEmpty()) {
            for (TicketComment c : ticketCommentRepository.findAllForTicketsWithAuthors(activeIds)) {
                commentsByTicket.computeIfAbsent(c.getTicket().getId(), k -> new ArrayList<>()).add(c);
            }
            for (List<TicketComment> lc : commentsByTicket.values()) {
                lc.sort(Comparator.comparing(TicketComment::getCreatedAt));
            }
        }

        List<Long> awaitingReplyIds = new ArrayList<>();
        int awaitingReplyCount = 0;
        for (Ticket t : activeAssigned) {
            List<TicketComment> thread = commentsByTicket.getOrDefault(t.getId(), List.of()).stream()
                    .filter(c -> !c.isInternal())
                    .toList();
            if (thread.isEmpty()) {
                continue;
            }
            TicketComment last = thread.get(thread.size() - 1);
            if (last.getAuthor() == null || last.getAuthor().getRole() != Role.ROLE_USER) {
                continue;
            }
            long hours = Duration.between(last.getCreatedAt(), LocalDateTime.now()).toHours();
            if (hours >= FOLLOWUP_WARN_HOURS) {
                awaitingReplyCount++;
                if (awaitingReplyIds.size() < MAX_EXAMPLE_IDS) {
                    awaitingReplyIds.add(t.getId());
                }
            }
        }

        List<AgentCoachTipDto> tips = new ArrayList<>();
        if (avgPickup != null && avgPickup >= PICKUP_WARN_HOURS) {
            tips.add(new AgentCoachTipDto(
                    "SLOW_STAGE",
                    avgPickup >= PICKUP_EXAMPLE_HOURS ? "warn" : "info",
                    "Pickup pace from Open → In progress",
                    String.format(Locale.US,
                            "Your average time before moving assigned tickets to In progress is about %.1f hours over the last %d days. "
                                    + "Aim to acknowledge and start work within one business day when possible.",
                            avgPickup, LOOKBACK_DAYS),
                    slowPickupExamples.isEmpty() ? List.of() : List.copyOf(slowPickupExamples)
            ));
        } else if (avgPickup != null && !slowPickupExamples.isEmpty()) {
            tips.add(new AgentCoachTipDto(
                    "SLOW_STAGE",
                    "info",
                    "A few tickets had a long Open phase",
                    "Some resolved tickets stayed in Open longer than two days before In progress. Short updates to the customer help even when work is queued.",
                    List.copyOf(slowPickupExamples)
            ));
        }

        if (avgInProgress != null && avgInProgress >= IN_PROGRESS_WARN_HOURS) {
            tips.add(new AgentCoachTipDto(
                    "SLOW_STAGE",
                    "warn",
                    "In progress duration",
                    String.format(Locale.US,
                            "Tickets spend about %.1f hours in In progress on average before resolution. "
                                    + "Consider smaller customer-facing milestones or ETA notes if work spans multiple days.",
                            avgInProgress),
                    slowProgressExamples.isEmpty() ? List.of() : List.copyOf(slowProgressExamples)
            ));
        }

        if (awaitingReplyCount > 0) {
            tips.add(new AgentCoachTipDto(
                    "FOLLOW_UP",
                    "warn",
                    "Customers may be waiting on your reply",
                    String.format(Locale.US,
                            "On %d active ticket(s) the latest message is from the customer and is older than about %.0f hours. "
                                    + "A quick acknowledgment or status update often improves satisfaction.",
                            awaitingReplyCount, FOLLOWUP_WARN_HOURS),
                    List.copyOf(awaitingReplyIds)
            ));
        }

        List<TicketComment> staffComments = ticketCommentRepository.findRecentByAuthorOnAssignedTickets(userId, userId, since);
        ToneScan tone = scanTone(staffComments);
        if (tone.issueCount >= 2 || tone.distinctPatterns >= 2) {
            tips.add(new AgentCoachTipDto(
                    "TONE",
                    tone.issueCount >= 4 ? "warn" : "info",
                    "Soften customer-facing wording",
                    "Some recent replies contained phrasing that can read as blunt or dismissive (for example stressing fault or using abrupt qualifiers). "
                            + "Lead with empathy, restate the issue, and state the next step.",
                    List.of()
            ));
        }

        if (tips.isEmpty()) {
            tips.add(new AgentCoachTipDto(
                    "SLOW_STAGE",
                    "info",
                    "Looking strong on the basics",
                    "No major timing, follow-up, or tone flags in your current assigned queue for this window. Keep using clear ETAs when work spans more than a day.",
                    List.of()
            ));
        }

        AgentCoachMetricsDto metrics = new AgentCoachMetricsDto(
                avgPickup,
                avgInProgress,
                coachTickets.size(),
                closedAnalyzed,
                awaitingReplyCount
        );
        return new AgentPerformanceCoachDto(List.copyOf(tips), metrics);
    }

    private static Double average(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private static StageDurations computeStageDurations(Ticket ticket, List<TicketChangeLog> orderedStatusLogs) {
        LocalDateTime created = ticket.getCreatedAt();
        if (created == null) {
            return null;
        }

        TicketStatus current = TicketStatus.OPEN;
        LocalDateTime firstInProgressAt = null;
        LocalDateTime resolvedAt = null;

        for (TicketChangeLog log : orderedStatusLogs) {
            TicketStatus newStatus = parseStatus(log.getNewValue());
            if (newStatus == null) {
                continue;
            }
            if (current == TicketStatus.IN_PROGRESS && (newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED)) {
                resolvedAt = log.getChangedAt();
            }
            if (newStatus == TicketStatus.IN_PROGRESS && firstInProgressAt == null) {
                firstInProgressAt = log.getChangedAt();
            }
            current = newStatus;
        }

        if (!CLOSED.contains(ticket.getStatus())) {
            return null;
        }

        Double pickupHours = null;
        if (firstInProgressAt != null) {
            pickupHours = hoursBetween(created, firstInProgressAt);
        }

        Double inProgressHours = null;
        if (firstInProgressAt != null && resolvedAt != null && resolvedAt.isAfter(firstInProgressAt)) {
            inProgressHours = hoursBetween(firstInProgressAt, resolvedAt);
        }

        if (pickupHours == null && inProgressHours == null) {
            return null;
        }
        return new StageDurations(pickupHours, inProgressHours);
    }

    private static TicketStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TicketStatus.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static double hoursBetween(LocalDateTime a, LocalDateTime b) {
        return Duration.between(a, b).toMinutes() / 60.0;
    }

    private static ToneScan scanTone(List<TicketComment> recentStaffComments) {
        int issueCount = 0;
        Set<String> matchedPatterns = new LinkedHashSet<>();
        int scanned = 0;
        for (TicketComment c : recentStaffComments) {
            if (scanned >= 40) {
                break;
            }
            scanned++;
            String body = c.getBody();
            if (body == null || body.isBlank()) {
                continue;
            }
            String lower = body.toLowerCase(Locale.ROOT);
            for (int i = 0; i < TONE_PATTERNS.size(); i++) {
                if (TONE_PATTERNS.get(i).matcher(lower).find()) {
                    issueCount++;
                    matchedPatterns.add("p" + i);
                    break;
                }
            }
            if (body.length() >= 40 && uppercaseLetterRatio(body) >= 0.45) {
                issueCount++;
                matchedPatterns.add("caps");
            }
        }
        return new ToneScan(issueCount, matchedPatterns.size());
    }

    /**
     * Share of A–Z letters that are uppercase (rough shoutiness signal).
     */
    private static double uppercaseLetterRatio(String body) {
        int upper = 0;
        int letters = 0;
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (Character.isLetter(ch)) {
                letters++;
                if (Character.isUpperCase(ch)) {
                    upper++;
                }
            }
        }
        return letters == 0 ? 0.0 : (double) upper / letters;
    }

    private record StageDurations(Double pickupHours, Double inProgressHours) {}

    private record ToneScan(int issueCount, int distinctPatterns) {}
}
