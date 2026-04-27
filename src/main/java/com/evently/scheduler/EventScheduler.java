package com.evently.scheduler;

import com.evently.model.Event;
import com.evently.model.EventStatus;
import com.evently.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task that auto-marks past published events as COMPLETED.
 *
 * Runs every day at midnight (server time).
 * The operation is idempotent — running it multiple times will not
 * corrupt already-completed or cancelled events.
 *
 * OWNER: Mohamed Morsy
 */
@Component
public class EventScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventScheduler.class);

    private final EventRepository eventRepository;

    public EventScheduler(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Cron: "0 0 0 * * *" = every day at 00:00:00.
     * Finds all PUBLISHED events whose dateTime is before now and marks them COMPLETED.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void markCompletedEvents() {
        List<Event> pastEvents = eventRepository
                .findByStatusAndDateTimeBefore(EventStatus.PUBLISHED, LocalDateTime.now());

        if (pastEvents.isEmpty()) {
            log.debug("EventScheduler: no past events to mark as completed.");
            return;
        }

        pastEvents.forEach(e -> e.setStatus(EventStatus.COMPLETED));
        eventRepository.saveAll(pastEvents);
        log.info("EventScheduler: marked {} event(s) as COMPLETED.", pastEvents.size());
    }
}
