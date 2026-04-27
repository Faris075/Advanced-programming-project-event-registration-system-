package com.evently;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Evently application.
 *
 * @EnableScheduling – activates the daily cron job that auto-completes past events.
 * @EnableAsync      – activates @Async on EmailService so mail delivery never blocks HTTP threads.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class EventlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventlyApplication.class, args);
    }
}
