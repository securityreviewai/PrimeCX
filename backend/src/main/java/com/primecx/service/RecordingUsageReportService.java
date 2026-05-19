package com.primecx.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.RecordingUsageSummaryDto;
import com.primecx.repository.RecordingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecordingUsageReportService {

    private final RecordingRepository recordingRepository;

    @Transactional(readOnly = true)
    public RecordingUsageSummaryDto buildSummary() {
        List<Object[]> rows = recordingRepository.aggregateUsageTotals();
        if (rows.isEmpty() || rows.get(0) == null) {
            return new RecordingUsageSummaryDto(0L, 0L, 0L);
        }
        Object[] row = rows.get(0);
        long count = row[0] instanceof Number n ? n.longValue() : 0L;
        long bytes = row[1] instanceof Number n ? n.longValue() : 0L;
        long duration = row[2] instanceof Number n ? n.longValue() : 0L;
        return new RecordingUsageSummaryDto(count, bytes, duration);
    }
}
