package com.primecx.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.RecordingDto;
import com.primecx.model.Recording;
import com.primecx.service.RecordingService;
import com.primecx.service.S3StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/recordings")
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingService recordingService;
    private final S3StorageService s3StorageService;

    @PostMapping("/upload-url")
    public ResponseEntity<Map<String, String>> generateUploadUrl(@RequestBody Map<String, Object> body) {
        Long sessionId = ((Number) body.get("sessionId")).longValue();
        String fileName = (String) body.get("fileName");
        String contentType = (String) body.get("contentType");

        String s3Key = s3StorageService.generateRecordingKey(sessionId, fileName);
        String presignedUrl = s3StorageService.generatePresignedUploadUrl(s3Key, contentType);

        return ResponseEntity.ok(Map.of(
                "uploadUrl", presignedUrl,
                "s3Key", s3Key
        ));
    }

    @PostMapping("/confirm")
    public ResponseEntity<RecordingDto> confirmUpload(@RequestBody Map<String, Object> body) {
        Long sessionId = ((Number) body.get("sessionId")).longValue();
        String s3Key = (String) body.get("s3Key");
        String fileName = (String) body.get("fileName");
        String contentType = (String) body.get("contentType");
        Long fileSize = ((Number) body.get("fileSize")).longValue();
        Integer duration = ((Number) body.get("duration")).intValue();

        Recording recording = recordingService.saveRecordingMetadata(
                sessionId, fileName, contentType, fileSize, duration, s3Key);
        return ResponseEntity.status(HttpStatus.CREATED).body(recordingService.toDto(recording));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<RecordingDto>> getRecordingsBySession(@PathVariable Long sessionId) {
        List<RecordingDto> dtos = recordingService.getRecordingsBySession(sessionId).stream()
                .map(r -> recordingService.toDto(r, false))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecordingDto> getRecordingById(@PathVariable Long id) {
        Recording recording = recordingService.getRecordingById(id);
        return ResponseEntity.ok(recordingService.toDto(recording, true));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPPORT_ADMIN')")
    public ResponseEntity<Void> deleteRecording(@PathVariable Long id) {
        recordingService.deleteRecording(id);
        return ResponseEntity.noContent().build();
    }
}
