package com.evently.repository;

import com.evently.model.Attendee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for the `attendees` table.
 *
 * OWNER: Faris
 * USED BY: Mohamed Ahmed (RegistrationService – find-or-create pattern)
 *          Islam (AdminRegistrationService – force-add)
 */
public interface AttendeeRepository extends JpaRepository<Attendee, Long> {

    /**
     * Finds an existing attendee contact record by email.
     * RegistrationService calls this before creating a new Attendee row
     * to avoid duplicate contact records for the same email address.
     */
    Optional<Attendee> findByEmail(String email);
}
