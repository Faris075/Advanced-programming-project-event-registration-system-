package com.evently.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Registration entity defaults and audit callbacks.
 * OWNER: Faris
 */
class RegistrationTest {

    @Test
    @DisplayName("Default status is CONFIRMED")
    void defaultStatus_isConfirmed() {
        Registration reg = new Registration();
        assertThat(reg.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Default paymentStatus is PENDING")
    void defaultPaymentStatus_isPending() {
        Registration reg = new Registration();
        assertThat(reg.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("isAdminOverride defaults to false")
    void isAdminOverride_defaultsFalse() {
        Registration reg = new Registration();
        assertThat(reg.isAdminOverride()).isFalse();
    }

    @Test
    @DisplayName("onCreate() sets registrationDate, createdAt, and updatedAt")
    void onCreate_setsAllAuditTimestamps() {
        Registration reg = new Registration();
        reg.onCreate();

        assertThat(reg.getRegistrationDate()).isNotNull();
        assertThat(reg.getCreatedAt()).isNotNull();
        assertThat(reg.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Builder sets event and attendee correctly")
    void builder_setsRelationships() {
        Event event = Event.builder().title("Conf").description("desc")
                .dateTime(java.time.LocalDateTime.now().plusDays(1))
                .location("Cairo").capacity(100)
                .status(EventStatus.PUBLISHED).build();

        Attendee attendee = Attendee.builder().name("Bob").email("bob@test.com").build();

        Registration reg = Registration.builder()
                .event(event)
                .attendee(attendee)
                .status(RegistrationStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        assertThat(reg.getEvent().getTitle()).isEqualTo("Conf");
        assertThat(reg.getAttendee().getEmail()).isEqualTo("bob@test.com");
    }
}
