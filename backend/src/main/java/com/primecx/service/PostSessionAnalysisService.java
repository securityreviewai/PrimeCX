package com.primecx.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.primecx.config.RecordingRetentionConfig;
import com.primecx.model.Recording;
import com.primecx.model.SupportSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostSessionAnalysisService {

    private final AIAnalysisService aiAnalysisService;
    private final RecordingTranscriptionService transcriptionService;
    private final RecordingRetentionConfig retentionConfig;
    private final RecordingService recordingService;

    @Async("analysisExecutor")
    public void analyzeSessionOnEnd(Long sessionId, String notes) {
        if (!retentionConfig.isAutoAnalysisOnSessionEnd()) {
            return;
        }
        try {
            String transcript = notes;
            if (transcript == null || transcript.isBlank()) {
                log.info("No session notes for session {}, skipping immediate analysis", sessionId);
                return;
            }
            aiAnalysisService.analyzeTranscript(sessionId, transcript);
            log.info("Auto-analysis completed for session {} from session notes", sessionId);
        } catch (Exception e) {
            log.error("Auto-analysis failed for session {}: {}", sessionId, e.getMessage(), e);
        }
    }

    @Async("analysisExecutor")
    public void transcribeAndAnalyzeRecording(Long recordingId) {
        if (!retentionConfig.isAutoTranscriptionEnabled()) {
            return;
        }
        try {
            Recording recording = recordingService.getRecordingById(recordingId);
            SupportSession session = recording.getSession();

            String transcript = recording.getTranscript();
            if (transcript == null || transcript.isBlank()) {
                transcript = transcriptionService.transcribeFromS3(
                        recording.getS3Key(), recording.getFileName());
                if (transcript != null && !transcript.isBlank()) {
                    recordingService.updateTranscript(recordingId, transcript);
                }
            }

            String textToAnalyze = transcript;
            if (textToAnalyze == null || textToAnalyze.isBlank()) {
                textToAnalyze = session.getNotes();
            }
            if (textToAnalyze == null || textToAnalyze.isBlank()) {
                log.info("No transcript or notes available for recording {}", recordingId);
                return;
            }

            aiAnalysisService.analyzeTranscript(session.getId(), textToAnalyze);
            log.info("Auto-analysis completed for recording {} / session {}", recordingId, session.getId());
        } catch (Exception e) {
            log.error("Transcribe-and-analyze failed for recording {}: {}", recordingId, e.getMessage(), e);
        }
    }
}
