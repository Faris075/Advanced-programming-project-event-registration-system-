package com.evently.controller;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.evently.dto.RegistrationFormDto;
import com.evently.exception.DuplicateRegistrationException;
import com.evently.model.Registration;
import com.evently.model.RegistrationStatus;
import com.evently.service.RegistrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Handles the public event registration form submission.
 * Requires authentication (covered by Spring Security's anyRequest().authenticated()).
 *
 * OWNER: Mohamed Ahmed
 */
@Controller
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * Process the registration form on the event detail page.
     * Redirects back to the event with a success or error flash message.
     */
    @PostMapping("/register/{eventId}")
    public String register(@PathVariable Long eventId,
                           @Valid @ModelAttribute("form") RegistrationFormDto form,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please fill in all required fields correctly.");
            return "redirect:/events/" + eventId;
        }

        try {
            Registration reg = registrationService.register(eventId, form);
            if (reg.getStatus() == RegistrationStatus.WAITLISTED) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "You're on the waitlist at position #" + reg.getWaitlistPosition()
                                + ". We'll notify you if a spot opens up.");
                return "redirect:/events/" + eventId;
            } else {
                // CONFIRMED → show payment page
                return "redirect:/register/payment/" + reg.getId();
            }
        } catch (DuplicateRegistrationException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You are already registered for this event.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/events/" + eventId;
    }
}
