package com.evently.controller.admin;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.evently.model.Registration;
import com.evently.model.RegistrationStatus;
import com.evently.repository.EventRepository;
import com.evently.repository.RegistrationRepository;
import com.evently.service.RegistrationService;

import lombok.RequiredArgsConstructor;

/**
 * Admin management of registrations: list, view, cancel, and status overrides.
 *
 * OWNER: Islam
 */
@Controller
@RequestMapping("/admin/registrations")
@RequiredArgsConstructor
public class AdminRegistrationController {

    private final RegistrationRepository registrationRepository;
    private final EventRepository        eventRepository;
    private final RegistrationService    registrationService;

    /** List all registrations, optionally filtered by event or status. */
    @GetMapping
    public String listRegistrations(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) RegistrationStatus status,
            Model model) {

        List<Registration> registrations;
        if (eventId != null && status != null) {
            registrations = registrationRepository.findByEventIdAndStatus(eventId, status);
        } else if (eventId != null) {
            registrations = registrationRepository.findByEventId(eventId);
        } else if (status != null) {
            registrations = registrationRepository.findAll(Sort.by("createdAt").descending())
                    .stream().filter(r -> r.getStatus() == status).toList();
        } else {
            registrations = registrationRepository.findAll(Sort.by("createdAt").descending());
        }

        model.addAttribute("registrations", registrations);
        model.addAttribute("events", eventRepository.findAll(Sort.by("title").ascending()));
        model.addAttribute("allStatuses", RegistrationStatus.values());
        model.addAttribute("selectedEventId", eventId);
        model.addAttribute("selectedStatus", status);
        return "admin/registrations/index";
    }

    /** Show a single registration with full detail. */
    @GetMapping("/{id}")
    public String showRegistration(@PathVariable Long id, Model model) {
        Registration reg = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + id));
        model.addAttribute("registration", reg);
        return "admin/registrations/show";
    }

    /** Cancel a registration and trigger waitlist promotion. */
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

    /** Manually override the status of a registration (admin power). */
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
}
