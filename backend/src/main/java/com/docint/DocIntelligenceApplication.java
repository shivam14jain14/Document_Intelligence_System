package com.docint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DocIntelligenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocIntelligenceApplication.class, args);
    }
}
