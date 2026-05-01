package com.evently.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the User entity.
 * Verifies Lombok builder, defaults, and audit callbacks.
 *
 * OWNER: Faris
 */
class UserTest {

    @Test
    @DisplayName("Builder creates user with correct fields")
    void builder_setsAllFields() {
        User user = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("$2a$12$hashed")
                .isAdmin(false)
                .currencyPreference("USD")
                .build();

        assertThat(user.getName()).isEqualTo("Alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getPassword()).isEqualTo("$2a$12$hashed");
        assertThat(user.isAdmin()).isFalse();
        assertThat(user.getCurrencyPreference()).isEqualTo("USD");
    }

    @Test
    @DisplayName("isAdmin defaults to false on new User")
    void isAdmin_defaultsFalse() {
        User user = new User();
        assertThat(user.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("currencyPreference defaults to USD")
    void currencyPreference_defaultsUSD() {
        User user = new User();
        assertThat(user.getCurrencyPreference()).isEqualTo("USD");
    }

    @Test
    @DisplayName("onCreate() sets both audit timestamps")
    void onCreate_setsTimestamps() {
        User user = new User();
        user.onCreate();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Admin user has isAdmin = true")
    void adminUser_hasIsAdminTrue() {
        User admin = User.builder()
                .name("Admin")
                .email("admin@evently.com")
                .password("$2a$12$adminHash")
                .isAdmin(true)
                .currencyPreference("USD")
                .build();

        assertThat(admin.isAdmin()).isTrue();
    }
}
