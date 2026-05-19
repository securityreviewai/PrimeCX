package com.primecx.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.TicketVolumeBucketDto;
import com.primecx.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketVolumeReportService {

    private final TicketRepository ticketRepository;

    /**
     * Daily counts of tickets created in each calendar day (timezone: server default).
     *
     * @param lookbackDays inclusive calendar-day span ending today (1–366).
     */
    @Transactional(readOnly = true)
    public List<TicketVolumeBucketDto> ticketsCreatedPerDay(int lookbackDays) {
        int days = Math.min(Math.max(lookbackDays, 1), 366);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();

        Map<LocalDate, Long> byDay = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            byDay.put(d, 0L);
        }

        List<Object[]> raw = ticketRepository.aggregateTicketsCreatedPerDay(from, to);
        for (Object[] row : raw) {
            LocalDate day = toLocalDate(row[0]);
            long cnt = row[1] instanceof Number n ? n.longValue() : 0L;
            if (day != null && byDay.containsKey(day)) {
                byDay.put(day, cnt);
            }
        }

        List<TicketVolumeBucketDto> out = new ArrayList<>(byDay.size());
        for (Map.Entry<LocalDate, Long> e : byDay.entrySet()) {
            out.add(new TicketVolumeBucketDto(e.getKey(), e.getValue()));
        }
        return out;
    }

    private static LocalDate toLocalDate(Object sqlOrDate) {
        if (sqlOrDate == null) {
            return null;
        }
        if (sqlOrDate instanceof LocalDate ld) {
            return ld;
        }
        if (sqlOrDate instanceof Date sd) {
            return sd.toLocalDate();
        }
        if (sqlOrDate instanceof java.util.Date ud) {
            return new Date(ud.getTime()).toLocalDate();
        }
        return null;
    }
}
