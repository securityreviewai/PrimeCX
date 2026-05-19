package com.primecx.repository;

import com.primecx.model.RecordingUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecordingUploadRepository extends JpaRepository<RecordingUpload, Long> {

    Optional<RecordingUpload> findByUploadId(String uploadId);

    Optional<RecordingUpload> findByS3KeyAndUploadId(String s3Key, String uploadId);
}
