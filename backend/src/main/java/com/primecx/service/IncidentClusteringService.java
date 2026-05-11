package com.primecx.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.EmergingIncidentClusterDto;
import com.primecx.dto.EmergingIncidentsOverviewDto;
import com.primecx.model.Ticket;
import com.primecx.model.TicketCategory;
import com.primecx.model.TicketStatus;
import com.primecx.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Groups recent open/in-progress tickets by textual similarity to surface emerging widespread issues.
 * Runs fully on-app (no external AI) using token + bigram Jaccard similarity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentClusteringService {

    private static final String METHOD = "text_jaccard_union_find_v1";
    private static final int MAX_WINDOW_HOURS = 168;
    private static final int MIN_WINDOW_HOURS = 1;
    private static final int MAX_MIN_CLUSTER = 20;
    private static final int MIN_MIN_CLUSTER = 2;
    private static final int MAX_SCAN_TICKETS = 1_000;
    private static final int MIN_SCAN_TICKETS = 50;
    private static final int MAX_CLUSTERS_RETURNED = 40;
    private static final int MAX_IDS_PER_CLUSTER = 25;
    private static final int MAX_SAMPLE_TITLES = 5;
    private static final int DESC_SNIPPET_CHARS = 2_000;

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "as", "by", "with",
            "from", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did",
            "will", "would", "could", "should", "may", "might", "must", "can", "not", "no", "yes", "i", "we", "you",
            "it", "this", "that", "these", "those", "my", "your", "our", "their", "what", "which", "who", "when",
            "where", "why", "how", "all", "each", "every", "both", "few", "more", "most", "other", "some", "such",
            "than", "too", "very", "just", "also", "only", "into", "about", "after", "before", "between", "through",
            "unable", "please", "help", "thanks", "thank", "hi", "hello"
    );

    private static final List<TicketStatus> ACTIVE_STATUSES = List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);

    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public EmergingIncidentsOverviewDto detectEmerging(
            int windowHours,
            int minClusterSize,
            double similarityThreshold,
            int maxTicketsToScan) {

        int wh = clamp(windowHours, MIN_WINDOW_HOURS, MAX_WINDOW_HOURS);
        int minC = clamp(minClusterSize, MIN_MIN_CLUSTER, MAX_MIN_CLUSTER);
        int cap = clamp(maxTicketsToScan, MIN_SCAN_TICKETS, MAX_SCAN_TICKETS);
        double thr = clamp(similarityThreshold, 0.12, 0.85);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusHours(wh);
        List<Ticket> tickets = ticketRepository.findActiveTicketsCreatedSince(
                since,
                ACTIVE_STATUSES,
                PageRequest.of(0, cap));

        if (tickets.size() < minC) {
            log.info("emerging_incidents skipped: tickets={} minCluster={}", tickets.size(), minC);
            return new EmergingIncidentsOverviewDto(
                    List.of(),
                    tickets.size(),
                    wh,
                    minC,
                    since,
                    now,
                    METHOD);
        }

        int n = tickets.size();
        List<Set<String>> signatures = new ArrayList<>(n);
        List<String> normTitles = new ArrayList<>(n);
        for (Ticket t : tickets) {
            normTitles.add(normalizeTitleKey(t.getTitle()));
            signatures.add(buildSignature(t));
        }

        UnionFind uf = new UnionFind(n);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (!normTitles.get(i).isEmpty() && normTitles.get(i).equals(normTitles.get(j))) {
                    uf.union(i, j);
                    continue;
                }
                if (jaccard(signatures.get(i), signatures.get(j)) >= thr) {
                    uf.union(i, j);
                }
            }
        }

        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int root = uf.find(i);
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(i);
        }

        List<List<Integer>> clusters = groups.values().stream()
                .filter(idxs -> idxs.size() >= minC)
                .sorted(Comparator.<List<Integer>, Integer>comparing(List::size).reversed()
                        .thenComparing(idxs -> newestCreatedAt(tickets, idxs), Comparator.reverseOrder()))
                .limit(MAX_CLUSTERS_RETURNED)
                .toList();

        List<EmergingIncidentClusterDto> dtos = new ArrayList<>();
        for (List<Integer> idxs : clusters) {
            dtos.add(toClusterDto(tickets, signatures, idxs));
        }

        log.info("emerging_incidents windowHours={} scanned={} clusters={}", wh, tickets.size(), dtos.size());

        return new EmergingIncidentsOverviewDto(
                dtos,
                tickets.size(),
                wh,
                minC,
                since,
                now,
                METHOD);
    }

    private static EmergingIncidentClusterDto toClusterDto(
            List<Ticket> tickets,
            List<Set<String>> signatures,
            List<Integer> idxs) {

        List<Ticket> members = idxs.stream().map(tickets::get).toList();
        List<Long> ids = members.stream().map(Ticket::getId).sorted().toList();

        LocalDateTime first = members.stream().map(Ticket::getCreatedAt).min(Comparator.naturalOrder()).orElse(null);
        LocalDateTime last = members.stream().map(Ticket::getCreatedAt).max(Comparator.naturalOrder()).orElse(null);

        Map<TicketCategory, Long> catCounts = members.stream()
                .map(t -> t.getCategory() != null ? t.getCategory() : TicketCategory.GENERAL)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
        TicketCategory dominant = catCounts.entrySet().stream()
                .max(Map.Entry.<TicketCategory, Long>comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(TicketCategory.GENERAL);

        List<String> titles = members.stream()
                .map(Ticket::getTitle)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .limit(MAX_SAMPLE_TITLES)
                .toList();

        double cohesion = averageIntraClusterJaccard(signatures, idxs);
        String risk = resolveRisk(members.size(), first, last, cohesion);

        String clusterId = "cl-" + Integer.toHexString(ids.hashCode());

        List<Long> idSample = ids.size() <= MAX_IDS_PER_CLUSTER ? ids : ids.subList(0, MAX_IDS_PER_CLUSTER);

        return new EmergingIncidentClusterDto(
                clusterId,
                members.size(),
                idSample,
                titles,
                dominant,
                first,
                last,
                risk,
                round2(cohesion));
    }

    private static double averageIntraClusterJaccard(List<Set<String>> signatures, List<Integer> idxs) {
        if (idxs.size() < 2) {
            return 1.0;
        }
        double sum = 0;
        int pairs = 0;
        int limitCompare = Math.min(idxs.size(), 9);
        for (int a = 0; a < limitCompare; a++) {
            for (int b = a + 1; b < limitCompare; b++) {
                sum += jaccard(signatures.get(idxs.get(a)), signatures.get(idxs.get(b)));
                pairs++;
            }
        }
        if (pairs == 0) {
            return 1.0;
        }
        return sum / pairs;
    }

    private static LocalDateTime newestCreatedAt(List<Ticket> tickets, List<Integer> idxs) {
        return idxs.stream()
                .map(tickets::get)
                .map(Ticket::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.MIN);
    }

    private static String resolveRisk(int size, LocalDateTime first, LocalDateTime last, double cohesion) {
        if (size >= 8) {
            return "HIGH";
        }
        if (size >= 5) {
            return "HIGH";
        }
        if (first != null && last != null && java.time.Duration.between(first, last).toHours() <= 3 && size >= 4) {
            return "HIGH";
        }
        if (size >= 4 && cohesion >= 0.45) {
            return "HIGH";
        }
        if (size >= 3) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String normalizeTitleKey(String title) {
        if (title == null) {
            return "";
        }
        String s = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
        return s.replaceAll("\\s+", " ");
    }

    private Set<String> buildSignature(Ticket ticket) {
        StringBuilder raw = new StringBuilder();
        if (ticket.getTitle() != null) {
            raw.append(ticket.getTitle()).append(' ');
        }
        if (ticket.getDescription() != null) {
            String d = ticket.getDescription();
            if (d.length() > DESC_SNIPPET_CHARS) {
                d = d.substring(0, DESC_SNIPPET_CHARS);
            }
            raw.append(d);
        }
        String normalized = raw.toString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ");
        String[] tokens = normalized.trim().split("\\s+");
        List<String> words = new ArrayList<>();
        for (String t : tokens) {
            if (t.length() < 2) {
                continue;
            }
            if (STOPWORDS.contains(t)) {
                continue;
            }
            words.add(t);
        }
        Set<String> sig = new HashSet<>(words);
        for (int i = 0; i + 1 < words.size(); i++) {
            sig.add(words.get(i) + "_" + words.get(i + 1));
        }
        return sig;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        Set<String> smaller = a.size() <= b.size() ? a : b;
        Set<String> larger = a.size() <= b.size() ? b : a;
        for (String s : smaller) {
            if (larger.contains(s)) {
                intersection++;
            }
        }
        int union = a.size() + b.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }

    private static final class UnionFind {
        private final int[] parent;
        private final int[] rank;

        UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
        }

        int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]);
            }
            return parent[x];
        }

        void union(int a, int b) {
            int ra = find(a);
            int rb = find(b);
            if (ra == rb) {
                return;
            }
            if (rank[ra] < rank[rb]) {
                parent[ra] = rb;
            } else if (rank[ra] > rank[rb]) {
                parent[rb] = ra;
            } else {
                parent[rb] = ra;
                rank[ra]++;
            }
        }
    }
}
