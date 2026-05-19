package com.primecx.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RecordingTranscriptionService {

    private final WebClient openAiWebClient;
    private final S3StorageService s3StorageService;
    private final ObjectMapper objectMapper;
    private final String whisperModel;

    public RecordingTranscriptionService(
            @Qualifier("openAiWebClient") WebClient openAiWebClient,
            S3StorageService s3StorageService,
            ObjectMapper objectMapper,
            @Value("${openai.whisper-model:whisper-1}") String whisperModel) {
        this.openAiWebClient = openAiWebClient;
        this.s3StorageService = s3StorageService;
        this.objectMapper = objectMapper;
        this.whisperModel = whisperModel;
    }

    public String transcribeFromS3(String s3Key, String fileName) {
        try {
            byte[] audioBytes = s3StorageService.getObjectBytes(s3Key);
            if (audioBytes.length == 0) {
                log.warn("Empty recording at key {}, skipping transcription", s3Key);
                return null;
            }

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return fileName != null ? fileName : "recording.webm";
                }
            }).contentType(MediaType.parseMediaType("video/webm"));
            builder.part("model", whisperModel);
            builder.part("response_format", "json");

            String responseBody = openAiWebClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("text").asText(null);
            log.info("Transcribed recording at key {}, length={}", s3Key, text != null ? text.length() : 0);
            return text;
        } catch (Exception e) {
            log.error("Transcription failed for key {}: {}", s3Key, e.getMessage(), e);
            return null;
        }
    }
}
