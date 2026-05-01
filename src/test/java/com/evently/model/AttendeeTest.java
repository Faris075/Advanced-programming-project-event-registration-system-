package com.evently.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Attendee entity defaults and Lombok builder.
 * OWNER: Faris
 */
class AttendeeTest {

    @Test
    @DisplayName("Builder sets name and email")
    void builder_setsNameAndEmail() {
        Attendee a = Attendee.builder()
                .name("Carol")
                .email("carol@example.com")
                .build();

        assertThat(a.getName()).isEqualTo("Carol");
        assertThat(a.getEmail()).isEqualTo("carol@example.com");
    }

    @Test
    @DisplayName("Phone and company are optional (can be null)")
    void phoneAndCompany_areOptional() {
        Attendee a = Attendee.builder()
                .name("Dan")
                .email("dan@example.com")
                .build();

        assertThat(a.getPhone()).isNull();
        assertThat(a.getCompany()).isNull();
    }

    @Test
    @DisplayName("onCreate() populates audit timestamps")
    void onCreate_setsTimestamps() {
        Attendee a = new Attendee();
        a.onCreate();

        assertThat(a.getCreatedAt()).isNotNull();
        assertThat(a.getUpdatedAt()).isNotNull();
    }
}
