package com.primecx.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.ResolutionTimeSummaryDto;
import com.primecx.dto.TicketCategoryMixDto;
import com.primecx.model.TicketCategory;
import com.primecx.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketAnalyticsReportService {

    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public List<TicketCategoryMixDto> ticketsCreatedByCategoryMix(int lookbackDays) {
        LocalDateTime[] win = reportingWindow(lookbackDays);
        EnumMap<TicketCategory, Long> counts = new EnumMap<>(TicketCategory.class);
        for (TicketCategory c : TicketCategory.values()) {
            counts.put(c, 0L);
        }
        List<Object[]> rows = ticketRepository.aggregateTicketsCreatedByCategory(win[0], win[1]);
        for (Object[] row : rows) {
            TicketCategory cat = parseCategory(row[0]);
            long n = row[1] instanceof Number num ? num.longValue() : 0L;
            if (cat != null) {
                counts.put(cat, n);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new TicketCategoryMixDto(e.getKey(), e.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResolutionTimeSummaryDto resolutionTimeSummary(int lookbackDays) {
        LocalDateTime[] win = reportingWindow(lookbackDays);
        List<Object[]> rows = ticketRepository.aggregateResolutionHoursForClosedTickets(win[0], win[1]);
        long closed = 0L;
        Double avgHours = null;
        if (!rows.isEmpty() && rows.get(0) != null) {
            Object[] row = rows.get(0);
            closed = row[0] instanceof Number n ? n.longValue() : 0L;
            if (row[1] instanceof Number n) {
                avgHours = n.doubleValue();
            }
        }
        return new ResolutionTimeSummaryDto(win[0], win[1], closed, avgHours);
    }

    /** Returns [{@code from}, {@code to}) as used elsewhere in reporting. */
    private static LocalDateTime[] reportingWindow(int lookbackDays) {
        int days = Math.min(Math.max(lookbackDays, 1), 366);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        return new LocalDateTime[] { start.atStartOfDay(), end.plusDays(1).atStartOfDay() };
    }

    private static TicketCategory parseCategory(Object raw) {
        if (raw == null) {
            return null;
        }
        String name = raw.toString().strip();
        if (name.isEmpty()) {
            return null;
        }
        try {
            return TicketCategory.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return TicketCategory.GENERAL_INQUIRY;
        }
    }
}
