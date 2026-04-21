package com.auca.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AucaPortalBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(AucaPortalBackendApplication.class, args);
    }
}