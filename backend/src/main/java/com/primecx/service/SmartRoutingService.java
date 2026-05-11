package com.primecx.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.model.CustomerTier;
import com.primecx.model.Role;
import com.primecx.model.Ticket;
import com.primecx.model.TicketCategory;
import com.primecx.model.TicketStatus;
import com.primecx.model.User;
import com.primecx.repository.TicketRepository;
import com.primecx.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scores active support executives using skill/category fit, current workload,
 * recent resolution history, and requester commercial tier.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartRoutingService {

    private static final int ROUTING_HISTORY_DAYS = 90;
    private static final Set<TicketStatus> ACTIVE_STATUSES = EnumSet.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
    private static final Set<TicketStatus> CLOSED_STATUSES = EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public Optional<User> pickAssignee(Ticket ticket) {
        User requester = ticket.getUser();
        if (requester == null) {
            return Optional.empty();
        }

        List<User> executives = userRepository.findActiveByRoleWithSupportSkills(Role.ROLE_SUPPORT_EXECUTIVE);
        if (executives.isEmpty()) {
            log.debug("Smart routing skipped: no active support executives");
            return Optional.empty();
        }

        CustomerTier tier = requester.getCustomerTier() != null ? requester.getCustomerTier() : CustomerTier.STANDARD;
        TicketCategory category = ticket.getCategory() != null ? ticket.getCategory() : TicketCategory.GENERAL;
        String categoryToken = category.name();

        List<Long> execIds = executives.stream().map(User::getId).toList();
        Map<Long, Long> activeWorkload = execIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> ticketRepository.countByAssignedToIdAndStatusIn(id, ACTIVE_STATUSES)
        ));
        long maxActive = activeWorkload.values().stream().mapToLong(Long::longValue).max().orElse(0L);

        LocalDateTime since = LocalDateTime.now().minusDays(ROUTING_HISTORY_DAYS);
        List<Ticket> closedRecent = ticketRepository.findClosedTicketsForAssigneesSince(execIds, CLOSED_STATUSES, since);
        Map<Long, List<Ticket>> closedByExec = closedRecent.stream()
                .filter(t -> t.getAssignedTo() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignedTo().getId()));

        double wSkill;
        double wLoad;
        double wQuality;
        switch (tier) {
            case ENTERPRISE -> {
                wSkill = 0.35;
                wLoad = 0.15;
                wQuality = 0.50;
            }
            case PREMIUM -> {
                wSkill = 0.30;
                wLoad = 0.25;
                wQuality = 0.45;
            }
            default -> {
                wSkill = 0.35;
                wLoad = 0.30;
                wQuality = 0.35;
            }
        }

        final double minQualityForEnterprise = tier == CustomerTier.ENTERPRISE ? 38.0 : 0.0;

        Optional<ScoredExecutive> best = executives.stream()
                .map(exec -> {
                    double skill = skillScore(exec, categoryToken);
                    double load = workloadScore(activeWorkload.getOrDefault(exec.getId(), 0L), maxActive);
                    double quality = qualityScore(closedByExec.getOrDefault(exec.getId(), List.of()));
                    return new ScoredExecutive(exec, skill, load, quality, composite(skill, load, quality, wSkill, wLoad, wQuality));
                })
                .filter(s -> s.qualityScore() >= minQualityForEnterprise || minQualityForEnterprise <= 0)
                .max(Comparator.comparingDouble(ScoredExecutive::composite));

        if (best.isEmpty() && tier == CustomerTier.ENTERPRISE) {
            best = executives.stream()
                    .map(exec -> {
                        double skill = skillScore(exec, categoryToken);
                        double load = workloadScore(activeWorkload.getOrDefault(exec.getId(), 0L), maxActive);
                        double quality = qualityScore(closedByExec.getOrDefault(exec.getId(), List.of()));
                        return new ScoredExecutive(exec, skill, load, quality, composite(skill, load, quality, wSkill, wLoad, wQuality));
                    })
                    .max(Comparator.comparingDouble(ScoredExecutive::composite));
        }

        return best.map(s -> {
            log.info(
                    "Smart routing chose executive {} (skill={} load={} quality={} composite={}) for ticket category {} tier {}",
                    s.executive().getId(),
                    round1(s.skillScore()),
                    round1(s.workloadScore()),
                    round1(s.qualityScore()),
                    round1(s.composite()),
                    category,
                    tier
            );
            return s.executive();
        });
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double composite(double skill, double load, double quality, double wS, double wL, double wQ) {
        return wS * skill + wL * load + wQ * quality;
    }

    private static double skillScore(User executive, String categoryToken) {
        Set<String> skills = executive.getSupportSkills();
        if (skills == null || skills.isEmpty()) {
            return 72.0;
        }
        if (skills.contains(categoryToken)) {
            return 100.0;
        }
        if (skills.contains(TicketCategory.GENERAL.name())) {
            return 82.0;
        }
        return 35.0;
    }

    private static double workloadScore(long activeCount, long maxActive) {
        if (maxActive <= 0) {
            return 100.0;
        }
        double norm = (double) (maxActive - activeCount) / (double) maxActive;
        return clamp(100.0 * norm, 0.0, 100.0);
    }

    private static double qualityScore(List<Ticket> closedTickets) {
        if (closedTickets.isEmpty()) {
            return 62.0;
        }
        double totalHours = 0.0;
        for (Ticket t : closedTickets) {
            if (t.getCreatedAt() != null && t.getUpdatedAt() != null) {
                totalHours += Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes() / 60.0;
            }
        }
        double avgHours = totalHours / closedTickets.size();
        double timeScore = 100.0 / (1.0 + avgHours / 36.0);
        double volumeScore = Math.min(100.0, closedTickets.size() * 4.0);
        return clamp(0.55 * timeScore + 0.45 * volumeScore, 0.0, 100.0);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private record ScoredExecutive(
            User executive,
            double skillScore,
            double workloadScore,
            double qualityScore,
            double composite
    ) {}
}
