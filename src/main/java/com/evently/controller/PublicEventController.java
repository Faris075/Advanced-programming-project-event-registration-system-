package com.evently.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.evently.dto.RegistrationFormDto;
import com.evently.exception.EventNotFoundException;
import com.evently.model.Event;
import com.evently.model.EventStatus;
import com.evently.repository.EventRepository;
import com.evently.repository.RegistrationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Serves the public event listing and event detail pages.
 * No authentication required for either endpoint.
 *
 * OWNER: Mohamed Ehab
 */
@Controller
@RequiredArgsConstructor
public class PublicEventController {

    private final EventRepository        eventRepository;
    private final RegistrationRepository registrationRepository;

    @GetMapping("/events")
    public String listEvents(@RequestParam(defaultValue = "0") int page,
            Model model, Authentication authentication) {
        Page<Event> events = eventRepository
                .findByStatusAndDateTimeAfterOrderByDateTimeAsc(EventStatus.PUBLISHED,
                        LocalDateTime.now(), PageRequest.of(page, 10));

        model.addAttribute("events", events);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", events.getTotalPages());

        java.util.Set<Long> registeredEventIds = Collections.emptySet();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            registeredEventIds = new HashSet<>(
                    registrationRepository.findActiveEventIdsByAttendeeEmail(authentication.getName()));
        }
        model.addAttribute("registeredEventIds", registeredEventIds);

        return "events/index";
    }

    @GetMapping("/events/{id}")
    public String showEvent(@PathVariable @NonNull Long id, Model model,
                            Authentication authentication) {
        Event event = eventRepository.findById(id)
            .orElseThrow(() -> new EventNotFoundException("Event not found: " + id));

        if (event.getStatus() != EventStatus.PUBLISHED
                || event.getDateTime().isBefore(LocalDateTime.now())) {
            return "redirect:/events";
        }

        long confirmedCount = eventRepository.countConfirmedRegistrations(id);
        boolean isSoldOut = confirmedCount >= event.getCapacity();
        long waitlistCount = registrationRepository.countWaitlisted(id);

        model.addAttribute("event", event);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("spotsLeft", event.getCapacity() - confirmedCount);
        model.addAttribute("isSoldOut", isSoldOut);
        model.addAttribute("waitlistCount", waitlistCount);
        model.addAttribute("form", new RegistrationFormDto());

        boolean isUserRegistered = false;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            String email = authentication.getName();
            isUserRegistered = registrationRepository
                    .countActiveByEventIdAndAttendeeEmail(id, email) > 0;
        }
        model.addAttribute("isUserRegistered", isUserRegistered);

        return "events/show";
    }
}
