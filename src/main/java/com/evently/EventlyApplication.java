package com.evently;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.cdimascio.dotenv.Dotenv;

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
        // Load .env into System properties before Spring's Environment is built.
        // ignoreIfMissing() makes this a no-op in CI / production where .env is absent.
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(e -> {
            if (System.getProperty(e.getKey()) == null) {
                System.setProperty(e.getKey(), e.getValue());
            }
        });

        SpringApplication.run(EventlyApplication.class, args);
    }
}
