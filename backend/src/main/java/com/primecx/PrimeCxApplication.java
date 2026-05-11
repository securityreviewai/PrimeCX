package com.primecx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.primecx.config.PrimecxStorageProperties;
import com.primecx.config.RecordingWebhookProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({RecordingWebhookProperties.class, PrimecxStorageProperties.class})
public class PrimeCxApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrimeCxApplication.class, args);
    }
}
