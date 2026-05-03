package com.evently.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.evently.model.Registration;
import com.evently.model.RegistrationStatus;

import jakarta.persistence.LockModeType;

/**
 * Data access for the `registrations` table.
 *
 * OWNER: Faris USED BY: Mohamed Ahmed (RegistrationService – core booking
 * logic) Islam (AdminRegistrationService – management + export)
 *
 * IMPORTANT: The pessimistic-lock query (findByIdWithLock) MUST be called from
 * within a @Transactional method to avoid "no transaction" exceptions.
 */
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    /**
     * Duplicate-prevention check before creating a new registration.
     */
    Optional<Registration> findByEventIdAndAttendeeId(Long eventId, Long attendeeId);

    /**
     * User's registration history, newest first.
     */
    List<Registration> findByAttendeeIdOrderByCreatedAtDesc(Long attendeeId);

    /**
     * All registrations for an event (admin listing).
     */
    List<Registration> findByEventId(Long eventId);

    /**
     * All registrations for an event filtered by status.
     */
    List<Registration> findByEventIdAndStatus(Long eventId, RegistrationStatus status);

    long countByStatus(RegistrationStatus status);

    /**
     * Returns waitlisted registrations for an event in promotion order. The
     * record at position 1 is the next attendee to be promoted.
     *
     * NOTE: Use the fully-qualified enum reference in JPQL — string literals
     * like 'WAITLISTED' are not valid JPQL and will cause a query parsing error
     * at startup.
     */
    @Query("SELECT r FROM Registration r "
            + "WHERE r.event.id = :eventId AND r.status = com.evently.model.RegistrationStatus.WAITLISTED "
            + "ORDER BY r.waitlistPosition ASC")
    List<Registration> findWaitlistedByEventIdOrdered(@Param("eventId") Long eventId);

    /**
     * Counts current waitlisted registrations for an event. Used to assign the
     * next sequential waitlistPosition.
     */
    @Query("SELECT COUNT(r) FROM Registration r "
            + "WHERE r.event.id = :eventId AND r.status = com.evently.model.RegistrationStatus.WAITLISTED")
    long countWaitlisted(@Param("eventId") Long eventId);

    /**
     * Pessimistic write lock on a single registration row.
     *
     * Use this inside a @Transactional method when reading the waitlist head
     * before promoting, to prevent two concurrent threads from promoting the
     * same record simultaneously (race condition prevention).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Registration r WHERE r.id = :id")
    Optional<Registration> findByIdWithLock(@Param("id") Long id);

    /**
     * Total registrations count across all events — used on admin dashboard.
     */
    @Query("SELECT COUNT(r) FROM Registration r")
    long countAll();
}
