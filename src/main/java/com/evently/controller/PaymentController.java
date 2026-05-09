package com.evently.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.evently.model.PaymentStatus;
import com.evently.repository.RegistrationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Serves the payment confirmation page after a successful registration.
 *
 * OWNER: Mohamed Ahmed
 */
@Controller
@RequestMapping("/register/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final RegistrationRepository registrationRepository;

    @GetMapping("/{registrationId}")
    public String paymentPage(@PathVariable Long registrationId,
                              Authentication authentication,
                              Model model) {
        if (authentication == null) {
            return "redirect:/auth/login";
        }

        String email = authentication.getName();

        return registrationRepository.findWithAssociationsById(registrationId)
                .filter(reg -> reg.getAttendee().getEmail().equalsIgnoreCase(email))
                .map(reg -> {
                    model.addAttribute("registration", reg);
                    return "register/payment";
                })
                .orElse("redirect:/dashboard");
    }

    @PostMapping("/{registrationId}/confirm")
    public String confirmPayment(@PathVariable Long registrationId,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/auth/login";
        }

        String email = authentication.getName();

        return registrationRepository.findWithAssociationsById(registrationId)
                .filter(reg -> reg.getAttendee().getEmail().equalsIgnoreCase(email))
                .map(reg -> {
                    reg.setPaymentStatus(PaymentStatus.PAID);
                    registrationRepository.save(reg);
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Payment confirmed! You're all set for " + reg.getEvent().getTitle() + ".");
                    return "redirect:/dashboard";
                })
                .orElse("redirect:/dashboard");
    }
}
