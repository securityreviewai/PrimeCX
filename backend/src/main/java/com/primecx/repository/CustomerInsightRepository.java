package com.primecx.repository;

import com.primecx.model.CustomerInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CustomerInsightRepository extends JpaRepository<CustomerInsight, Long> {

    List<CustomerInsight> findByUserId(Long userId);

    List<CustomerInsight> findByInsightType(String insightType);

    List<CustomerInsight> findByValidUntilAfter(LocalDateTime dateTime);

    List<CustomerInsight> findByUserIdAndInsightType(Long userId, String insightType);

    List<CustomerInsight> findTop20ByOrderByCreatedAtDesc();
}
