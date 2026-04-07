package com.primecx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PrimeCxApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrimeCxApplication.class, args);
    }
}
