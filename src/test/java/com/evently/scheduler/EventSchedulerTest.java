package com.evently.scheduler;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.evently.model.Event;
import com.evently.model.EventStatus;
import com.evently.repository.EventRepository;

/**
 * Integration tests for EventScheduler.
 *
 * Uses @DataJpaTest + @Import to load only the JPA slice and the scheduler component.
 * Calls markCompletedEvents() directly (no scheduler timer involved).
 *
 * OWNER: Faris (scheduler set up by Morsy)
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(EventScheduler.class)
@SuppressWarnings("null")
class EventSchedulerTest {

    @Autowired private EventScheduler  eventScheduler;
    @Autowired private EventRepository eventRepository;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
    }

    @Test
    @DisplayName("markCompletedEvents() marks PUBLISHED past events as COMPLETED")
    void markCompletedEvents_marksPastPublishedEvents() {
        // Arrange: one past PUBLISHED event
        eventRepository.save(buildEvent("Past Event", LocalDateTime.now().minusDays(1), EventStatus.PUBLISHED));

        // Act
        eventScheduler.markCompletedEvents();

        // Assert
        List<Event> events = eventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getStatus()).isEqualTo(EventStatus.COMPLETED);
    }

    @Test
    @DisplayName("markCompletedEvents() does NOT change future events")
    void markCompletedEvents_leaveFutureEventsUntouched() {
        eventRepository.save(buildEvent("Future Event", LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED));

        eventScheduler.markCompletedEvents();

        List<Event> events = eventRepository.findAll();
        assertThat(events.get(0).getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("markCompletedEvents() does NOT change CANCELLED or DRAFT events")
    void markCompletedEvents_doesNotTouchNonPublishedEvents() {
        eventRepository.save(buildEvent("Draft Past", LocalDateTime.now().minusDays(1), EventStatus.DRAFT));
        eventRepository.save(buildEvent("Cancelled Past", LocalDateTime.now().minusDays(1), EventStatus.CANCELLED));

        eventScheduler.markCompletedEvents();

        List<Event> allEvents = eventRepository.findAll();
        // Both should keep their original status
        assertThat(allEvents).noneMatch(e -> e.getStatus() == EventStatus.COMPLETED);
    }

    @Test
    @DisplayName("markCompletedEvents() is safe when called with no events")
    void markCompletedEvents_emptyDB_doesNotThrow() {
        // Should complete without any exception
        org.assertj.core.api.Assertions.assertThatCode(
                () -> eventScheduler.markCompletedEvents()
        ).doesNotThrowAnyException();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private Event buildEvent(String title, LocalDateTime dateTime, EventStatus status) {
        return Event.builder()
                .title(title)
                .description("Scheduler test description")
                .dateTime(dateTime)
                .location("Test Location")
                .capacity(50)
                .price(BigDecimal.ZERO)
                .status(status)
                .build();
    }
}
