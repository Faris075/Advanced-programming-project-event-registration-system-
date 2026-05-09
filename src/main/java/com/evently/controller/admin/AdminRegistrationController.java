package com.evently.controller.admin;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.evently.model.Event;
import com.evently.model.Registration;
import com.evently.model.RegistrationStatus;
import com.evently.repository.EventRepository;
import com.evently.repository.RegistrationRepository;
import com.evently.service.AdminRegistrationService;
import com.evently.service.RegistrationService;

import lombok.RequiredArgsConstructor;

/**
 * Admin management of registrations: list, view, export, and force-add.
 *
 * OWNER: Islam
 */
@Controller
@RequestMapping("/admin/registrations")
@RequiredArgsConstructor
public class AdminRegistrationController {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final RegistrationService registrationService;
    private final AdminRegistrationService adminRegistrationService;

    @GetMapping
    public String listRegistrations(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) RegistrationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        Page<Registration> registrations = adminRegistrationService.getPage(
                eventId, status, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("registrations", registrations);
        model.addAttribute("events", eventRepository.findAll(Sort.by("title").ascending()));
        model.addAttribute("allStatuses", RegistrationStatus.values());
        model.addAttribute("selectedEventId", eventId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", registrations.getTotalPages());
        model.addAttribute("pageSize", size);
        return "admin/registrations/index";
    }

    @GetMapping("/{id}")
    public String showRegistration(@PathVariable Long id, Model model) {
        Registration reg = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + id));
        model.addAttribute("registration", reg);
        model.addAttribute("allStatuses", RegistrationStatus.values());
        return "admin/registrations/show";
    }

    @PostMapping("/{id}/cancel")
    public String cancelRegistration(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            registrationService.cancel(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration #" + id + " cancelled and waitlist updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/registrations";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
            @RequestParam RegistrationStatus status,
            RedirectAttributes redirectAttributes) {
        Registration reg = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + id));
        reg.setStatus(status);
        if (status != RegistrationStatus.WAITLISTED) {
            reg.setWaitlistPosition(null);
        }
        registrationRepository.save(reg);
        redirectAttributes.addFlashAttribute("successMessage", "Status updated to " + status + ".");
        return "redirect:/admin/registrations/" + id;
    }

    @GetMapping("/event/{eventId}")
    public String showEventRegistrations(@PathVariable @NonNull Long eventId,
            @RequestParam(required = false) RegistrationStatus status,
            Model model) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        List<Registration> registrations = status != null
                ? adminRegistrationService.getByEventAndStatus(eventId, status)
                : adminRegistrationService.getByEvent(eventId);

        model.addAttribute("event", event);
        model.addAttribute("registrations", registrations);
        model.addAttribute("allStatuses", RegistrationStatus.values());
        model.addAttribute("selectedStatus", status);
        return "admin/registrations/event";
    }

    @PostMapping("/event/{eventId}/force-add")
    public String forceAdd(@PathVariable Long eventId,
            @RequestParam String attendeeEmail,
            RedirectAttributes redirectAttributes) {
        try {
            adminRegistrationService.forceAdd(eventId, attendeeEmail);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Attendee " + attendeeEmail + " has been force-added as confirmed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/registrations/event/" + eventId;
    }

    @GetMapping("/event/{eventId}/export.csv")
    public ResponseEntity<String> exportCsv(@PathVariable Long eventId) {
        String csv = adminRegistrationService.exportCsv(eventId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=registrations-event-" + eventId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
