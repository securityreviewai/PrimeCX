package com.primecx.service;

import com.primecx.dto.CustomerInsightDto;
import com.primecx.model.CustomerInsight;
import com.primecx.model.User;
import com.primecx.repository.CustomerInsightRepository;
import com.primecx.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerInsightService {

    private final CustomerInsightRepository customerInsightRepository;
    private final UserRepository userRepository;

    public List<CustomerInsightDto> getInsightsForUser(Long userId) {
        return customerInsightRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public List<CustomerInsightDto> getRecentInsights() {
        return customerInsightRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    public List<CustomerInsightDto> getInsightsByType(String type) {
        return customerInsightRepository.findByInsightType(type).stream()
                .map(this::toDto)
                .toList();
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void deleteExpiredInsights() {
        List<CustomerInsight> expired = customerInsightRepository.findAll().stream()
                .filter(i -> i.getValidUntil() != null && i.getValidUntil().isBefore(LocalDateTime.now()))
                .toList();

        if (!expired.isEmpty()) {
            customerInsightRepository.deleteAll(expired);
            log.info("Deleted {} expired customer insights", expired.size());
        }
    }

    public CustomerInsightDto toDto(CustomerInsight insight) {
        User user = insight.getUser();
        String userName = user.getFirstName() + " " + user.getLastName();
        return new CustomerInsightDto(
                insight.getId(),
                user.getId(),
                userName,
                insight.getInsightType(),
                insight.getTitle(),
                insight.getDescription(),
                insight.getData(),
                insight.getConfidenceScore(),
                insight.getValidUntil(),
                insight.getCreatedAt()
        );
    }
}
