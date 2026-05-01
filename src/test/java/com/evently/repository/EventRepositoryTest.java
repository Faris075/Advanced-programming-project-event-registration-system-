package com.evently.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.evently.model.Event;
import com.evently.model.EventStatus;

/**
 * Integration tests for EventRepository using the H2 in-memory database.
 * Covers the custom query methods Faris defined.
 *
 * OWNER: Faris
 */
@DataJpaTest
@ActiveProfiles("test")
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // findByStatusOrderByDateTimeAsc
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findByStatusOrderByDateTimeAsc returns only PUBLISHED events, sorted ascending")
    void findByStatus_returnsOnlyPublished_sortedByDateTime() {
        // Arrange: two published events (different dates) + one draft
        eventRepository.save(buildEvent("Later", LocalDateTime.now().plusDays(10), EventStatus.PUBLISHED));
        eventRepository.save(buildEvent("Sooner", LocalDateTime.now().plusDays(2), EventStatus.PUBLISHED));
        eventRepository.save(buildEvent("Draft Event", LocalDateTime.now().plusDays(5), EventStatus.DRAFT));

        // Act
        Page<Event> page = eventRepository.findByStatusOrderByDateTimeAsc(
                EventStatus.PUBLISHED, PageRequest.of(0, 10));

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(2);
        // First element should be the sooner event (ascending order)
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Sooner");
        assertThat(page.getContent().get(1).getTitle()).isEqualTo("Later");
    }

    @Test
    @DisplayName("findByStatusOrderByDateTimeAsc returns empty page when no published events")
    void findByStatus_noPublishedEvents_returnsEmptyPage() {
        eventRepository.save(buildEvent("Draft", LocalDateTime.now().plusDays(1), EventStatus.DRAFT));

        Page<Event> page = eventRepository.findByStatusOrderByDateTimeAsc(
                EventStatus.PUBLISHED, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isZero();
    }

    // -----------------------------------------------------------------------
    // findByStatusAndDateTimeBefore
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findByStatusAndDateTimeBefore returns PUBLISHED events in the past")
    void findByStatusAndDateTimeBefore_returnsPastPublishedEvents() {
        // Past event (should be returned)
        eventRepository.save(buildEvent("Past Event", LocalDateTime.now().minusDays(1), EventStatus.PUBLISHED));
        // Future event (should NOT be returned)
        eventRepository.save(buildEvent("Future Event", LocalDateTime.now().plusDays(1), EventStatus.PUBLISHED));
        // Past but DRAFT (should NOT be returned)
        eventRepository.save(buildEvent("Past Draft", LocalDateTime.now().minusDays(1), EventStatus.DRAFT));

        List<Event> results = eventRepository
                .findByStatusAndDateTimeBefore(EventStatus.PUBLISHED, LocalDateTime.now());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Past Event");
    }

    @Test
    @DisplayName("findByStatusAndDateTimeBefore returns empty list when no past published events")
    void findByStatusAndDateTimeBefore_noMatch_returnsEmpty() {
        eventRepository.save(buildEvent("Future", LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED));

        List<Event> results = eventRepository
                .findByStatusAndDateTimeBefore(EventStatus.PUBLISHED, LocalDateTime.now());

        assertThat(results).isEmpty();
    }

    // -----------------------------------------------------------------------
    // countConfirmedRegistrations — tested via RegistrationRepositoryTest
    // because we need Attendee + Registration persisted in the same context.
    // This test just verifies the query returns 0 for a fresh event.
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("countConfirmedRegistrations returns 0 for new event with no registrations")
    void countConfirmedRegistrations_noRegistrations_returnsZero() {
        Event event = eventRepository.save(
                buildEvent("Empty Event", LocalDateTime.now().plusDays(1), EventStatus.PUBLISHED));

        long count = eventRepository.countConfirmedRegistrations(event.getId());

        assertThat(count).isZero();
    }

    // -----------------------------------------------------------------------
    // countAdminOverrides
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("countAdminOverrides returns 0 for new event")
    void countAdminOverrides_noOverrides_returnsZero() {
        Event event = eventRepository.save(
                buildEvent("Overrides Test", LocalDateTime.now().plusDays(1), EventStatus.PUBLISHED));

        long count = eventRepository.countAdminOverrides(event.getId());

        assertThat(count).isZero();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /** Creates a minimal valid Event entity for testing. */
    private Event buildEvent(String title, LocalDateTime dateTime, EventStatus status) {
        return Event.builder()
                .title(title)
                .description("Test description for " + title)
                .dateTime(dateTime)
                .location("Test Location")
                .capacity(100)
                .price(new BigDecimal("0.00"))
                .status(status)
                .build();
    }
}
