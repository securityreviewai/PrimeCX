package com.primecx.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.primecx.model.CannedResponse;

@Repository
public interface CannedResponseRepository extends JpaRepository<CannedResponse, Long> {

    Optional<CannedResponse> findByShortcode(String shortcode);

    List<CannedResponse> findByCategory(String category);

    List<CannedResponse> findAllByOrderByShortcodeAsc();

    boolean existsByShortcode(String shortcode);
}
