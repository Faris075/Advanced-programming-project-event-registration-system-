package com.evently.repository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.evently.model.Attendee;

/**
 * Integration tests for AttendeeRepository using H2.
 * Focuses on the findByEmail method used by the find-or-create pattern.
 *
 * OWNER: Faris
 */
@DataJpaTest
@ActiveProfiles("test")
@SuppressWarnings("null")
class AttendeeRepositoryTest {

    @Autowired
    private AttendeeRepository attendeeRepository;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
    }

    @Test
    @DisplayName("findByEmail returns attendee when email exists")
    void findByEmail_whenExists_returnsAttendee() {
        Attendee saved = attendeeRepository.save(buildAttendee("eve@test.com"));

        Optional<Attendee> result = attendeeRepository.findByEmail("eve@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getName()).isEqualTo("Eve");
    }

    @Test
    @DisplayName("findByEmail returns empty for unknown email")
    void findByEmail_whenNotExists_returnsEmpty() {
        Optional<Attendee> result = attendeeRepository.findByEmail("nobody@nowhere.com");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Email is unique – saving duplicate email throws")
    void save_duplicateEmail_throwsException() {
        attendeeRepository.save(buildAttendee("dup@test.com"));

        // Attempting to save a second row with the same email should violate the UNIQUE constraint.
        Attendee duplicate = buildAttendee("dup@test.com");

        // DataIntegrityViolationException wraps the constraint violation at the JPA layer.
        org.assertj.core.api.ThrowableAssert.ThrowingCallable attempt =
                () -> {
                    attendeeRepository.save(duplicate);
                    attendeeRepository.flush(); // force the INSERT to hit the DB
                };

        org.assertj.core.api.Assertions.assertThatException()
                .isThrownBy(attempt)
                .isInstanceOfAny(
                        org.springframework.dao.DataIntegrityViolationException.class,
                        jakarta.persistence.PersistenceException.class
                );
    }

    @Test
    @DisplayName("Optional phone and company fields are persisted when provided")
    void save_withPhoneAndCompany_persistsOptionalFields() {
        Attendee attendee = Attendee.builder()
                .name("Frank")
                .email("frank@test.com")
                .phone("+1-555-0100")
                .company("ACME Corp")
                .build();

        Attendee saved = attendeeRepository.save(attendee);
        Attendee loaded = attendeeRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getPhone()).isEqualTo("+1-555-0100");
        assertThat(loaded.getCompany()).isEqualTo("ACME Corp");
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private Attendee buildAttendee(String email) {
        return Attendee.builder()
                .name("Eve")
                .email(email)
                .build();
    }
}
