package com.evently.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Event entity helper methods and defaults.
 *
 * These are pure Java tests – no Spring context needed.
 * OWNER: Faris
 */
class EventTest {

    // -----------------------------------------------------------------------
    // isFree()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("isFree() returns true when price is null")
    void isFree_whenPriceNull_returnsTrue() {
        Event event = Event.builder()
                .title("Test")
                .description("desc")
                .dateTime(LocalDateTime.now().plusDays(1))
                .location("Somewhere")
                .capacity(50)
                .price(null)
                .status(EventStatus.PUBLISHED)
                .build();

        assertThat(event.isFree()).isTrue();
    }

    @Test
    @DisplayName("isFree() returns true when price is zero")
    void isFree_whenPriceZero_returnsTrue() {
        Event event = Event.builder()
                .title("Free Event")
                .description("desc")
                .dateTime(LocalDateTime.now().plusDays(1))
                .location("Somewhere")
                .capacity(50)
                .price(BigDecimal.ZERO)
                .status(EventStatus.PUBLISHED)
                .build();

        assertThat(event.isFree()).isTrue();
    }

    @Test
    @DisplayName("isFree() returns false when price is positive")
    void isFree_whenPricePositive_returnsFalse() {
        Event event = Event.builder()
                .title("Paid Event")
                .description("desc")
                .dateTime(LocalDateTime.now().plusDays(1))
                .location("Somewhere")
                .capacity(50)
                .price(new BigDecimal("50.00"))
                .status(EventStatus.PUBLISHED)
                .build();

        assertThat(event.isFree()).isFalse();
    }

    // -----------------------------------------------------------------------
    // Default status
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Default status is DRAFT when built without explicit status")
    void defaultStatus_isDraft() {
        Event event = new Event();
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    // -----------------------------------------------------------------------
    // @PrePersist audit timestamps
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("onCreate() sets both createdAt and updatedAt")
    void onCreate_setsAuditTimestamps() {
        Event event = new Event();
        event.onCreate(); // invoke lifecycle callback directly

        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getUpdatedAt()).isNotNull();
        // Both should be very close to now
        assertThat(event.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("onUpdate() refreshes updatedAt without touching createdAt")
    void onUpdate_refreshesUpdatedAt() throws InterruptedException {
        Event event = new Event();
        event.onCreate();
        LocalDateTime originalCreatedAt = event.getCreatedAt();

        // Small sleep so updatedAt will differ from createdAt
        Thread.sleep(10);
        event.onUpdate();

        assertThat(event.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(event.getUpdatedAt()).isAfterOrEqualTo(originalCreatedAt);
    }
}
