package com.evently.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Exposes the BCryptPasswordEncoder as a Spring bean so it can be injected
 * anywhere without creating circular dependencies between SecurityConfig and
 * other beans that need password hashing (e.g. DataSeeder, AuthController).
 *
 * OWNER: Faris
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 12 gives a good balance between security and hashing time.
        return new BCryptPasswordEncoder(12);
    }
}
