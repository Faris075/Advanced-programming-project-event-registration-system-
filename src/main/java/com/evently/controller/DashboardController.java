package com.evently.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.evently.repository.AttendeeRepository;
import com.evently.repository.RegistrationRepository;
import com.evently.service.RegistrationService;

import lombok.RequiredArgsConstructor;

/**
 * Shows the authenticated user's registration history and lets them cancel.
 *
 * OWNER: Mohamed Ahmed
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AttendeeRepository     attendeeRepository;
    private final RegistrationRepository registrationRepository;
    private final RegistrationService    registrationService;

    @GetMapping({"", "/"})
    public String dashboard(Authentication authentication, Model model) {
        String email = authentication.getName().trim().toLowerCase();
        attendeeRepository.findByEmail(email).ifPresent(attendee ->
                model.addAttribute("registrations",
                        registrationRepository.findByAttendeeIdOrderByCreatedAtDesc(attendee.getId()))
        );
        if (!model.containsAttribute("registrations")) {
            model.addAttribute("registrations", List.of());
        }
        return "dashboard";
    }

    @PostMapping("/cancel/{registrationId}")
    public String cancel(@PathVariable Long registrationId,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        // Verify the registration belongs to the current user before cancelling.
        String email = authentication.getName().trim().toLowerCase();
        registrationRepository.findById(registrationId).ifPresent(reg -> {
            if (reg.getAttendee().getEmail().equalsIgnoreCase(email)) {
                try {
                    registrationService.cancel(registrationId);
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Your registration has been cancelled.");
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "You are not authorised to cancel this registration.");
            }
        });
        return "redirect:/dashboard";
    }
}
