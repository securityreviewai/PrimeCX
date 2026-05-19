package com.primecx.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.CompletedPartDto;
import com.primecx.dto.CreateQaReviewRequest;
import com.primecx.dto.MultipartCompleteRequest;
import com.primecx.dto.MultipartInitRequest;
import com.primecx.dto.MultipartInitResponse;
import com.primecx.dto.MultipartPartUrlRequest;
import com.primecx.dto.MultipartPartUrlResponse;
import com.primecx.dto.RecordingAccessAuditDto;
import com.primecx.dto.RecordingDto;
import com.primecx.dto.RecordingPlaybackDto;
import com.primecx.dto.RecordingQaReviewDto;
import com.primecx.dto.RetentionPolicyDto;
import com.primecx.dto.SaveRedactionsRequest;
import com.primecx.model.Recording;
import com.primecx.model.User;
import com.primecx.service.RecordingQaService;
import com.primecx.service.RecordingService;
import com.primecx.service.S3StorageService;
import com.primecx.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/recordings")
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingService recordingService;
    private final RecordingQaService recordingQaService;
    private final S3StorageService s3StorageService;
    private final UserService userService;

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

    @PostMapping("/multipart/init")
    @PreAuthorize("hasRole('SUPPORT_EXECUTIVE')")
    public ResponseEntity<MultipartInitResponse> initMultipart(@RequestBody MultipartInitRequest request) {
        return ResponseEntity.ok(recordingService.initiateMultipartUpload(request));
    }

    @PostMapping("/multipart/part-url")
    @PreAuthorize("hasRole('SUPPORT_EXECUTIVE')")
    public ResponseEntity<MultipartPartUrlResponse> getPartUrl(@RequestBody MultipartPartUrlRequest request) {
        return ResponseEntity.ok(recordingService.getMultipartPartUrl(request));
    }

    @PostMapping("/multipart/complete")
    @PreAuthorize("hasRole('SUPPORT_EXECUTIVE')")
    public ResponseEntity<RecordingDto> completeMultipart(@RequestBody Map<String, Object> body) {
        MultipartCompleteRequest request = mapCompleteRequest(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(recordingService.completeMultipartUpload(request));
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
        recordingService.triggerPostUploadAnalysis(recording.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(recordingService.toDto(recording));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<RecordingDto>> getRecordingsBySession(@PathVariable Long sessionId) {
        List<RecordingDto> dtos = recordingService.getRecordingsBySession(sessionId).stream()
                .map(r -> recordingService.toDto(r, false))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/retention-policy")
    public ResponseEntity<RetentionPolicyDto> getRetentionPolicy() {
        return ResponseEntity.ok(recordingService.getRetentionPolicy());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecordingDto> getRecordingById(@PathVariable Long id) {
        Recording recording = recordingService.getRecordingById(id);
        return ResponseEntity.ok(recordingService.toDto(recording, true));
    }

    @GetMapping("/{id}/playback")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<RecordingPlaybackDto> getPlayback(
            @PathVariable Long id,
            @AuthenticationPrincipal OidcUser oidcUser,
            HttpServletRequest httpRequest) {
        User user = userService.getUserByOktaId(oidcUser.getSubject());
        String ip = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(recordingService.getPlayback(id, user, ip));
    }

    @PutMapping("/{id}/redactions")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<RecordingDto> saveRedactions(
            @PathVariable Long id,
            @RequestBody SaveRedactionsRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User user = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(recordingService.saveRedactions(id, request, user));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<List<RecordingAccessAuditDto>> getAuditLog(
            @PathVariable Long id,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User user = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.ok(recordingService.getAccessAudit(id, user));
    }

    @PostMapping("/{id}/qa-reviews")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<RecordingQaReviewDto> createQaReview(
            @PathVariable Long id,
            @RequestBody CreateQaReviewRequest request,
            @AuthenticationPrincipal OidcUser oidcUser) {
        User user = userService.getUserByOktaId(oidcUser.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordingQaService.createReview(id, request, user));
    }

    @GetMapping("/{id}/qa-reviews")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<List<RecordingQaReviewDto>> getQaReviews(@PathVariable Long id) {
        return ResponseEntity.ok(recordingQaService.getReviewsForRecording(id));
    }

    @GetMapping("/qa-reviews/recent")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<List<RecordingQaReviewDto>> getRecentQaReviews() {
        return ResponseEntity.ok(recordingQaService.getRecentReviews());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPPORT_ADMIN')")
    public ResponseEntity<Void> deleteRecording(@PathVariable Long id) {
        recordingService.deleteRecording(id);
        return ResponseEntity.noContent().build();
    }

    @SuppressWarnings("unchecked")
    private MultipartCompleteRequest mapCompleteRequest(Map<String, Object> body) {
        Long sessionId = ((Number) body.get("sessionId")).longValue();
        String s3Key = (String) body.get("s3Key");
        String uploadId = (String) body.get("uploadId");
        String fileName = (String) body.get("fileName");
        String contentType = (String) body.get("contentType");
        Long fileSize = body.get("fileSize") != null ? ((Number) body.get("fileSize")).longValue() : 0L;
        Integer duration = body.get("duration") != null ? ((Number) body.get("duration")).intValue() : 0;

        List<Map<String, Object>> rawParts = (List<Map<String, Object>>) body.get("parts");
        List<CompletedPartDto> parts = rawParts.stream()
                .map(p -> new CompletedPartDto(
                        ((Number) p.get("partNumber")).intValue(),
                        (String) p.get("etag")))
                .collect(Collectors.toList());

        return new MultipartCompleteRequest(
                sessionId, s3Key, uploadId, fileName, contentType, fileSize, duration, parts);
    }
}
