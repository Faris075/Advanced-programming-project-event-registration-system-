package com.evently.controller.admin;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.evently.dto.EventDto;
import com.evently.exception.EventNotFoundException;
import com.evently.model.Event;
import com.evently.model.EventStatus;
import com.evently.model.Registration;
import com.evently.model.RegistrationStatus;
import com.evently.repository.EventRepository;
import com.evently.repository.RegistrationRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    @GetMapping
    public String listEvents(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Page<Event> events = eventRepository.findAll(
                PageRequest.of(page, size, Sort.by("dateTime").ascending())
        );

        model.addAttribute("events", events);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", events.getTotalPages());
        return "admin/events/index";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("eventDto", new EventDto());
        return "admin/events/create";
    }

    @PostMapping("/create")
    public String createEvent(@Valid EventDto eventDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/events/create";
        }

        Event event = fromDto(eventDto);
        event.setStatus(EventStatus.DRAFT);
        eventRepository.save(event);

        redirectAttributes.addFlashAttribute("success", "Event created successfully.");
        return "redirect:/admin/events";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        model.addAttribute("eventDto", toDto(event));
        model.addAttribute("eventId", id);
        return "admin/events/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateEvent(@PathVariable Long id,
            @Valid EventDto eventDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        if (bindingResult.hasErrors()) {
            model.addAttribute("eventId", id);
            return "admin/events/edit";
        }

        existingEvent.setTitle(eventDto.getTitle());
        existingEvent.setDescription(eventDto.getDescription());
        existingEvent.setDateTime(eventDto.getDateTime());
        existingEvent.setLocation(eventDto.getLocation());
        existingEvent.setCapacity(eventDto.getCapacity());
        existingEvent.setPrice(eventDto.getPrice());
        eventRepository.save(existingEvent);

        redirectAttributes.addFlashAttribute("success", "Event updated successfully.");
        return "redirect:/admin/events";
    }

    @PostMapping("/{id}/publish")
    public String publishEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        if (event.getStatus() != EventStatus.DRAFT) {
            redirectAttributes.addFlashAttribute("error", "Only draft events can be published.");
            return "redirect:/admin/events";
        }

        event.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event);
        redirectAttributes.addFlashAttribute("success", "Event published successfully.");
        return "redirect:/admin/events";
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public String cancelEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);

        List<Registration> confirmed = registrationRepository.findByEventIdAndStatus(id, RegistrationStatus.CONFIRMED);
        List<Registration> waitlisted = registrationRepository.findByEventIdAndStatus(id, RegistrationStatus.WAITLISTED);

        List<Registration> toUpdate = new ArrayList<>();
        toUpdate.addAll(confirmed);
        toUpdate.addAll(waitlisted);

        toUpdate.forEach(r -> r.setStatus(RegistrationStatus.CANCELLED));
        registrationRepository.saveAll(toUpdate);

        redirectAttributes.addFlashAttribute("success", "Event cancelled and related registrations updated.");
        return "redirect:/admin/events";
    }

    @PostMapping("/{id}/delete")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        if (event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.CANCELLED) {
            redirectAttributes.addFlashAttribute("error", "Only draft or cancelled events can be deleted.");
            return "redirect:/admin/events";
        }

        eventRepository.delete(event);
        redirectAttributes.addFlashAttribute("success", "Event deleted successfully.");
        return "redirect:/admin/events";
    }

    private Event fromDto(EventDto dto) {
        return Event.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dateTime(dto.getDateTime())
                .location(dto.getLocation())
                .capacity(dto.getCapacity())
                .price(dto.getPrice())
                .status(dto.getStatus() != null ? dto.getStatus() : EventStatus.DRAFT)
                .build();
    }

    private EventDto toDto(Event event) {
        EventDto dto = new EventDto();
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setDateTime(event.getDateTime());
        dto.setLocation(event.getLocation());
        dto.setCapacity(event.getCapacity());
        dto.setPrice(event.getPrice());
        dto.setStatus(event.getStatus());
        return dto;
    }
}
