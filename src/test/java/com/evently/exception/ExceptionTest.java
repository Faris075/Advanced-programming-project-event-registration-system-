package com.evently.exception;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the custom exception classes.
 * Verifies message construction and HTTP status annotations.
 *
 * OWNER: Faris
 */
class ExceptionTest {

    // -----------------------------------------------------------------------
    // EventNotFoundException
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("EventNotFoundException(Long) builds correct message")
    void eventNotFoundException_longCtor_buildsMessage() {
        EventNotFoundException ex = new EventNotFoundException(42L);
        assertThat(ex.getMessage()).contains("42");
    }

    @Test
    @DisplayName("EventNotFoundException(String) uses provided message")
    void eventNotFoundException_stringCtor_usesMessage() {
        EventNotFoundException ex = new EventNotFoundException("Custom not found message");
        assertThat(ex.getMessage()).isEqualTo("Custom not found message");
    }

    @Test
    @DisplayName("EventNotFoundException is a RuntimeException")
    void eventNotFoundException_isRuntimeException() {
        assertThat(new EventNotFoundException(1L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("EventNotFoundException carries @ResponseStatus(404)")
    void eventNotFoundException_hasNotFoundStatus() {
        org.springframework.http.HttpStatus status = EventNotFoundException.class
                .getAnnotation(org.springframework.web.bind.annotation.ResponseStatus.class)
                .value();
        assertThat(status).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    // -----------------------------------------------------------------------
    // DuplicateRegistrationException
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("DuplicateRegistrationException message includes eventId and email")
    void duplicateRegistrationException_includesEventIdAndEmail() {
        DuplicateRegistrationException ex = new DuplicateRegistrationException(7L, "user@test.com");
        assertThat(ex.getMessage()).contains("7");
        assertThat(ex.getMessage()).contains("user@test.com");
    }

    @Test
    @DisplayName("DuplicateRegistrationException is a RuntimeException")
    void duplicateRegistrationException_isRuntimeException() {
        assertThat(new DuplicateRegistrationException(1L, "a@b.com"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("DuplicateRegistrationException carries @ResponseStatus(409)")
    void duplicateRegistrationException_hasConflictStatus() {
        org.springframework.http.HttpStatus status = DuplicateRegistrationException.class
                .getAnnotation(org.springframework.web.bind.annotation.ResponseStatus.class)
                .value();
        assertThat(status).isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
    }
}
