package com.primecx.repository;

import com.primecx.model.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordingRepository extends JpaRepository<Recording, Long> {

    List<Recording> findBySessionId(Long sessionId);

    List<Recording> findBySession_SupportExecutiveId(Long supportExecutiveId);

    List<Recording> findBySession_Ticket_IdOrderByUploadedAtDesc(Long ticketId);

    @Query(value = """
            SELECT COUNT(*),
                   COALESCE(SUM(file_size), 0),
                   COALESCE(SUM(COALESCE(duration_seconds, 0)), 0)
            FROM recordings
            """, nativeQuery = true)
    List<Object[]> aggregateUsageTotals();
}
