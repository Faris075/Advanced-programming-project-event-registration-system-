package com.evently.controller;

import com.evently.dto.RegistrationFormDto;
import com.evently.exception.EventNotFoundException;
import com.evently.model.Event;
import com.evently.model.EventStatus;
import com.evently.repository.EventRepository;
import com.evently.repository.RegistrationRepository;
import com.evently.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
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
 */
@Controller
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private static final int PAGE_SIZE = 10;

    private final EventRepository        eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository         userRepository;

    @GetMapping
    public String listEvents(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Event> events = eventRepository.findByStatusOrderByDateTimeAsc(
                EventStatus.PUBLISHED,
                PageRequest.of(page, PAGE_SIZE, Sort.by("dateTime").ascending())
        );
        model.addAttribute("events", events);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", events.getTotalPages());
        return "events/index";
    }

    @GetMapping("/{id}")
    public String showEvent(@PathVariable Long id, Model model, Authentication authentication) {
        Event event = eventRepository.findById(id)
            .orElseThrow(() -> new EventNotFoundException("Event not found: " + id));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            return "redirect:/events";
        }

        long confirmedCount = eventRepository.countConfirmedRegistrations(id);
        long waitlistCount = registrationRepository.countWaitlisted(id);
        boolean isSoldOut = confirmedCount >= event.getCapacity();

        model.addAttribute("event", event);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("spotsLeft", Math.max(0, event.getCapacity() - confirmedCount));
        model.addAttribute("isSoldOut", isSoldOut);
        model.addAttribute("waitlistCount", waitlistCount);
        model.addAttribute("form", new RegistrationFormDto());

        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            userRepository.findByEmail(email).ifPresent(user -> {
                model.addAttribute("isUserRegistered", false);
            });
        }

        return "events/show";
    }
}
