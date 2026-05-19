package com.primecx.repository;

import com.primecx.model.RecordingAccessAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordingAccessAuditRepository extends JpaRepository<RecordingAccessAudit, Long> {

    List<RecordingAccessAudit> findByRecordingIdOrderByAccessedAtDesc(Long recordingId);
}
