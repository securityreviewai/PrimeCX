package com.primecx.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecordingRetentionConfig {

    @Value("${primecx.recordings.retention-days:365}")
    private int retentionDays;

    @Value("${primecx.recordings.transition-to-ia-days:30}")
    private int transitionToIaDays;

    @Value("${primecx.recordings.transition-to-glacier-days:90}")
    private int transitionToGlacierDays;

    @Value("${primecx.recordings.auto-transcription-enabled:true}")
    private boolean autoTranscriptionEnabled;

    @Value("${primecx.recordings.auto-analysis-on-session-end:true}")
    private boolean autoAnalysisOnSessionEnd;

    public int getRetentionDays() {
        return retentionDays;
    }

    public int getTransitionToIaDays() {
        return transitionToIaDays;
    }

    public int getTransitionToGlacierDays() {
        return transitionToGlacierDays;
    }

    public boolean isAutoTranscriptionEnabled() {
        return autoTranscriptionEnabled;
    }

    public boolean isAutoAnalysisOnSessionEnd() {
        return autoAnalysisOnSessionEnd;
    }
}
