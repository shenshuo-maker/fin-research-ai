package com.finresearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FinResearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinResearchApplication.class, args);
    }
}
