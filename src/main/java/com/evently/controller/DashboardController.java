package com.evently.controller;

import java.util.List;

import org.springframework.lang.NonNull;
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
import lombok.extern.slf4j.Slf4j;

/**
 * Shows the authenticated user's registration history and lets them cancel.
 *
 * OWNER: Mohamed Ahmed
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final AttendeeRepository     attendeeRepository;
    private final RegistrationRepository registrationRepository;
    private final RegistrationService    registrationService;


    

    @GetMapping({"", "/"})
    public String dashboard(Authentication authentication, Model model) {



if (authentication == null) {
            return "redirect:/login";
        }



        String email = authentication.getName();
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
    public String cancel(@PathVariable @NonNull Long registrationId,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {

 if (authentication == null) {
            return "redirect:/login";
        }



        // Verify the registration belongs to the current user before cancelling.
        // Use findWithAssociationsById to avoid LazyInitializationException on reg.getAttendee()
        // when open-in-view is false.
        String email = authentication.getName();
        var optReg = registrationRepository.findWithAssociationsById(registrationId);
        if (optReg.isEmpty()) {
            log.warn("Cancel attempt for non-existent registration {} by {}", registrationId, email);
            redirectAttributes.addFlashAttribute("errorMessage", "Registration not found.");
            return "redirect:/dashboard";
        }

        var reg = optReg.get();
        if (!reg.getAttendee().getEmail().equalsIgnoreCase(email)) {
            log.warn("Unauthorised cancel attempt: registration {} belongs to {}, requested by {}",
                    registrationId, reg.getAttendee().getEmail(), email);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You are not authorised to cancel this registration.");
            return "redirect:/dashboard";
        }

        try {
            registrationService.cancel(registrationId);
            log.info("Registration {} cancelled by {}", registrationId, email);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your registration has been cancelled.");
        } catch (Exception e) {
            log.error("Failed to cancel registration {} for {}: {}", registrationId, email, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    e.getMessage() != null ? e.getMessage() : "An unexpected error occurred. Please try again.");
        }
        return "redirect:/dashboard";
    }
}
