package com.evently.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.evently.model.Attendee;
import com.evently.model.Event;
import com.evently.model.EventStatus;
import com.evently.model.PaymentStatus;
import com.evently.model.Registration;
import com.evently.model.RegistrationStatus;

/**
 * Integration tests for RegistrationRepository using H2.
 * Verifies custom JPQL queries, the waitlist ordering query,
 * and the pessimistic-lock method.
 *
 * OWNER: Faris
 */
@DataJpaTest
@ActiveProfiles("test")
class RegistrationRepositoryTest {

    @Autowired private RegistrationRepository registrationRepository;
    @Autowired private EventRepository        eventRepository;
    @Autowired private AttendeeRepository     attendeeRepository;

    private Event     savedEvent;
    private Attendee  attendeeA;
    private Attendee  attendeeB;

    /**
     * Persist a shared event and two attendees that tests can use.
     * Registrations are created per-test to avoid interdependencies.
     */
    @BeforeEach
    void setUp() {
        registrationRepository.deleteAll();
        attendeeRepository.deleteAll();
        eventRepository.deleteAll();

        savedEvent = eventRepository.save(Event.builder()
                .title("Test Event")
                .description("desc")
                .dateTime(LocalDateTime.now().plusDays(7))
                .location("Cairo")
                .capacity(5)
                .price(BigDecimal.ZERO)
                .status(EventStatus.PUBLISHED)
                .build());

        attendeeA = attendeeRepository.save(Attendee.builder()
                .name("Alice").email("alice@repo.test").build());

        attendeeB = attendeeRepository.save(Attendee.builder()
                .name("Bob").email("bob@repo.test").build());
    }

    // -----------------------------------------------------------------------
    // findByEventIdAndAttendeeId
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findByEventIdAndAttendeeId returns existing registration")
    void findByEventAndAttendee_whenExists_returnsRegistration() {
        registrationRepository.save(buildRegistration(attendeeA, RegistrationStatus.CONFIRMED, null));

        Optional<Registration> result = registrationRepository
                .findByEventIdAndAttendeeId(savedEvent.getId(), attendeeA.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getAttendee().getId()).isEqualTo(attendeeA.getId());
    }

    @Test
    @DisplayName("findByEventIdAndAttendeeId returns empty when no match")
    void findByEventAndAttendee_whenNotExists_returnsEmpty() {
        Optional<Registration> result = registrationRepository
                .findByEventIdAndAttendeeId(savedEvent.getId(), attendeeB.getId());

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // findWaitlistedByEventIdOrdered
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findWaitlistedByEventIdOrdered returns waitlisted records in position order")
    void findWaitlisted_returnsInPositionOrder() {
        // Insert two waitlisted registrations out of order
        registrationRepository.save(buildRegistration(attendeeA, RegistrationStatus.WAITLISTED, 2));
        registrationRepository.save(buildRegistration(attendeeB, RegistrationStatus.WAITLISTED, 1));

        List<Registration> waitlist = registrationRepository
                .findWaitlistedByEventIdOrdered(savedEvent.getId());

        assertThat(waitlist).hasSize(2);
        // Position 1 should come first (ascending order)
        assertThat(waitlist.get(0).getWaitlistPosition()).isEqualTo(1);
        assertThat(waitlist.get(1).getWaitlistPosition()).isEqualTo(2);
    }

    @Test
    @DisplayName("findWaitlistedByEventIdOrdered excludes CONFIRMED and CANCELLED registrations")
    void findWaitlisted_excludesNonWaitlisted() {
        registrationRepository.save(buildRegistration(attendeeA, RegistrationStatus.CONFIRMED, null));

        List<Registration> waitlist = registrationRepository
                .findWaitlistedByEventIdOrdered(savedEvent.getId());

        assertThat(waitlist).isEmpty();
    }

    // -----------------------------------------------------------------------
    // countWaitlisted
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("countWaitlisted returns correct count")
    void countWaitlisted_returnsCorrectCount() {
        registrationRepository.save(buildRegistration(attendeeA, RegistrationStatus.WAITLISTED, 1));
        registrationRepository.save(buildRegistration(attendeeB, RegistrationStatus.WAITLISTED, 2));

        long count = registrationRepository.countWaitlisted(savedEvent.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countWaitlisted returns 0 when no waitlisted registrations")
    void countWaitlisted_noWaitlisted_returnsZero() {
        registrationRepository.save(buildRegistration(attendeeA, RegistrationStatus.CONFIRMED, null));

        long count = registrationRepository.countWaitlisted(savedEvent.getId());

        assertThat(count).isZero();
    }

    // -----------------------------------------------------------------------
    // countAll
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("countAll returns total registrations across the system")
    void countAll_returnsTotal() {
        registrationRepository.save(buildRegistration(attendeeA, RegistrationStatus.CONFIRMED, null));
        registrationRepository.save(buildRegistration(attendeeB, RegistrationStatus.WAITLISTED, 1));

        long total = registrationRepository.countAll();

        assertThat(total).isEqualTo(2);
    }

    // -----------------------------------------------------------------------
    // findByIdWithLock (pessimistic lock – must run inside a transaction)
    // -----------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findByIdWithLock returns registration inside a transaction")
    void findByIdWithLock_returnsRegistration() {
        Registration saved = registrationRepository.save(
                buildRegistration(attendeeA, RegistrationStatus.CONFIRMED, null));

        Optional<Registration> locked = registrationRepository.findByIdWithLock(saved.getId());

        assertThat(locked).isPresent();
        assertThat(locked.get().getId()).isEqualTo(saved.getId());
    }

    // -----------------------------------------------------------------------
    // countConfirmedRegistrations via EventRepository
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("countConfirmedRegistrations via EventRepository counts only CONFIRMED status")
    void countConfirmedRegistrations_onlyCountsConfirmed() {
        registrationRepository.save(buildRegistration(attendeeA, RegistrationStatus.CONFIRMED, null));
        registrationRepository.save(buildRegistration(attendeeB, RegistrationStatus.WAITLISTED, 1));

        long confirmed = eventRepository.countConfirmedRegistrations(savedEvent.getId());

        assertThat(confirmed).isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /** Builds a Registration linking attendee to the shared savedEvent. */
    private Registration buildRegistration(Attendee attendee,
                                           RegistrationStatus status,
                                           Integer waitlistPosition) {
        return Registration.builder()
                .event(savedEvent)
                .attendee(attendee)
                .status(status)
                .paymentStatus(PaymentStatus.PENDING)
                .waitlistPosition(waitlistPosition)
                .build();
    }
}
