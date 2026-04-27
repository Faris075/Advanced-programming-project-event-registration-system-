package com.evently.controller;

import com.evently.model.EventStatus;
import com.evently.repository.EventRepository;
import com.evently.repository.RegistrationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Serves the public event listing and event detail pages.
 * No authentication required for either endpoint.
 *
 * OWNER: Mohamed Ehab
 *
 * TODO (Ehab):
 *   1. GET /events — fetch paginated PUBLISHED events, pass page to model.
 *   2. GET /events/{id} — load single event; 404 if not found or not PUBLISHED.
 *      Pass to model: confirmedCount, remainingSpots, isFullyBooked,
 *      isAlreadyRegistered (check against logged-in user if authenticated).
 */
@Controller
@RequestMapping("/events")
public class PublicEventController {

    private static final int PAGE_SIZE = 10;

    private final EventRepository        eventRepository;
    private final RegistrationRepository registrationRepository;

    public PublicEventController(EventRepository eventRepository,
                                 RegistrationRepository registrationRepository) {
        this.eventRepository        = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    /**
     * TODO (Ehab): Implement the events listing.
     *
     * - Default page = 0, sorted by dateTime ASC.
     * - Pass to model: events (Page<Event>), currentPage, totalPages.
     * - Template: events/index.html
     */
    @GetMapping
    public String listEvents(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<?> events = eventRepository.findByStatusOrderByDateTimeAsc(
                EventStatus.PUBLISHED,
                PageRequest.of(page, PAGE_SIZE, Sort.by("dateTime").ascending())
        );
        model.addAttribute("events", events);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", events.getTotalPages());
        return "events/index";
    }

    /**
     * TODO (Ehab): Implement the event detail page.
     *
     * - Throw EventNotFoundException (→ 404) if event doesn't exist or isn't PUBLISHED.
     * - Compute confirmedCount = eventRepository.countConfirmedRegistrations(id).
     * - Compute remainingSpots = event.getCapacity() - confirmedCount (min 0).
     * - Check isAlreadyRegistered by looking up the authenticated user's attendee record.
     * - Template: events/show.html
     */
    @GetMapping("/{id}")
    public String showEvent(@PathVariable Long id, Model model) {
        // TODO (Ehab): implement
        return "events/show";
    }
}
