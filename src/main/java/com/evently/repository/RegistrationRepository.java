package com.evently.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
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
     * User's registration history, newest first. Eagerly loads event and attendee
     * to avoid LazyInitializationException when open-in-view is false.
     */
    @EntityGraph(attributePaths = {"event", "attendee"})
    List<Registration> findByAttendeeIdOrderByCreatedAtDesc(Long attendeeId);

    /**
     * All registrations for an event (admin listing). Eager-loads associations.
     */
    @EntityGraph(attributePaths = {"event", "attendee"})
    List<Registration> findByEventId(Long eventId);

    @EntityGraph(attributePaths = {"event", "attendee"})
    Page<Registration> findByEventId(Long eventId, org.springframework.data.domain.Pageable pageable);

    /**
     * All registrations for an event filtered by status. Eager-loads associations.
     */
    @EntityGraph(attributePaths = {"event", "attendee"})
    List<Registration> findByEventIdAndStatus(Long eventId, RegistrationStatus status);

    @EntityGraph(attributePaths = {"event", "attendee"})
    Page<Registration> findByEventIdAndStatus(Long eventId, RegistrationStatus status, org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"event", "attendee"})
    Page<Registration> findByStatus(RegistrationStatus status, org.springframework.data.domain.Pageable pageable);

    /**
     * Override findAll to eagerly load associations for admin listing pages.
     */
    @EntityGraph(attributePaths = {"event", "attendee"})
    @Override
    Page<Registration> findAll(org.springframework.data.domain.Pageable pageable);

    /**
     * Load a single registration with attendee eagerly (used for ownership checks
     * in DashboardController without an open Hibernate session).
     */
    @EntityGraph(attributePaths = {"attendee", "event"})
    @Query("SELECT r FROM Registration r WHERE r.id = :id")
    Optional<Registration> findWithAssociationsById(@Param("id") Long id);

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
     * Counts confirmed registrations for an event. Used to check capacity.
     */
    @Query("SELECT COUNT(r) FROM Registration r "
            + "WHERE r.event.id = :eventId AND r.status = com.evently.model.RegistrationStatus.CONFIRMED")
    long countConfirmedRegistrations(@Param("eventId") Long eventId);

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

    /**
     * Checks if a logged-in user (matched by attendee email) already has a
     * non-cancelled registration for the given event.
     */
    @Query("SELECT COUNT(r) FROM Registration r "
            + "WHERE r.event.id = :eventId AND r.attendee.email = :email "
            + "AND r.status <> com.evently.model.RegistrationStatus.CANCELLED")
    long countActiveByEventIdAndAttendeeEmail(@Param("eventId") Long eventId, @Param("email") String email);

    /**
     * Returns all event IDs for which the given attendee email has a
     * non-cancelled registration. Used on the events list to mark already-registered cards.
     */
    @Query("SELECT r.event.id FROM Registration r "
            + "WHERE r.attendee.email = :email "
            + "AND r.status <> com.evently.model.RegistrationStatus.CANCELLED")
    List<Long> findActiveEventIdsByAttendeeEmail(@Param("email") String email);
}
