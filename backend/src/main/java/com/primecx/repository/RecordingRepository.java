package com.primecx.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.primecx.model.Recording;

@Repository
public interface RecordingRepository extends JpaRepository<Recording, Long> {

    List<Recording> findBySessionId(Long sessionId);

    List<Recording> findBySessionIdAndDeletedAtIsNull(Long sessionId);

    List<Recording> findBySession_SupportExecutiveId(Long supportExecutiveId);

    Optional<Recording> findByIdAndSession_SupportExecutive_Id(Long id, Long supportExecutiveId);

    /**
     * Keyset pagination for purge scans (avoids skipped rows when the current page shrinks after deletes).
     */
    org.springframework.data.domain.Slice<Recording> findByIdGreaterThanAndLegalHoldIsFalseOrderByIdAsc(
            Long id, Pageable pageable);
}
