package com.evently.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit tests for the three enum types: EventStatus, RegistrationStatus, PaymentStatus.
 * Verifies enum constants match the database ENUM values exactly (case-insensitive
 * mapping is handled by @Enumerated(EnumType.STRING) at the Hibernate layer).
 *
 * OWNER: Faris
 */
class EnumTest {

    // -----------------------------------------------------------------------
    // EventStatus
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("EventStatus has exactly 4 values")
    void eventStatus_hasFourValues() {
        assertThat(EventStatus.values()).hasSize(4);
    }

    @ParameterizedTest
    @EnumSource(EventStatus.class)
    @DisplayName("All EventStatus constants are accessible by name()")
    void eventStatus_allAccessibleByName(EventStatus status) {
        assertThat(EventStatus.valueOf(status.name())).isSameAs(status);
    }

    @Test
    @DisplayName("EventStatus contains DRAFT, PUBLISHED, CANCELLED, COMPLETED")
    void eventStatus_containsExpectedConstants() {
        assertThat(EventStatus.values())
                .containsExactlyInAnyOrder(
                        EventStatus.DRAFT,
                        EventStatus.PUBLISHED,
                        EventStatus.CANCELLED,
                        EventStatus.COMPLETED
                );
    }

    // -----------------------------------------------------------------------
    // RegistrationStatus
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("RegistrationStatus has exactly 3 values")
    void registrationStatus_hasThreeValues() {
        assertThat(RegistrationStatus.values()).hasSize(3);
    }

    @Test
    @DisplayName("RegistrationStatus contains CONFIRMED, WAITLISTED, CANCELLED")
    void registrationStatus_containsExpectedConstants() {
        assertThat(RegistrationStatus.values())
                .containsExactlyInAnyOrder(
                        RegistrationStatus.CONFIRMED,
                        RegistrationStatus.WAITLISTED,
                        RegistrationStatus.CANCELLED
                );
    }

    // -----------------------------------------------------------------------
    // PaymentStatus
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PaymentStatus has exactly 3 values")
    void paymentStatus_hasThreeValues() {
        assertThat(PaymentStatus.values()).hasSize(3);
    }

    @Test
    @DisplayName("PaymentStatus contains PENDING, PAID, REFUNDED")
    void paymentStatus_containsExpectedConstants() {
        assertThat(PaymentStatus.values())
                .containsExactlyInAnyOrder(
                        PaymentStatus.PENDING,
                        PaymentStatus.PAID,
                        PaymentStatus.REFUNDED
                );
    }
}
