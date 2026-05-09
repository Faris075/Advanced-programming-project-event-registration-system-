package com.evently.controller.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.evently.model.User;
import com.evently.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * Manages the authenticated user's profile.
 *
 * OWNER: Alei
 *
 * Endpoints:
 *   - GET  /profile            → show profile page (current user's name, email, currency)
 *   - POST /profile/update     → update name (email is read-only here, can be changed via account settings)
 *   - POST /profile/password   → verify current password, save new BCrypt hash
 *   - POST /profile/currency   → update preferred currency code
 *   - POST /profile/delete     → verify password confirmation, delete account, invalidate session
 *
 * Use SecurityContextHolder to get the authenticated user's email, then load the User entity.
 */
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get the currently logged-in user from the database.
     */
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }

    @GetMapping
    public String profilePage(Model model) {
        User user = getCurrentUser();
        model.addAttribute("user", user);
        return "auth/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam String name,
                                 RedirectAttributes redirectAttributes) {
        if (name == null || name.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Name cannot be empty.");
            return "redirect:/profile";
        }

        User user = getCurrentUser();
        user.setName(name.trim());
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success",
                "Profile updated successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword,
                                  @RequestParam String newPassword,
                                  @RequestParam String passwordConfirm,
                                  RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error",
                    "Current password is incorrect.");
            return "redirect:/profile";
        }

        // Validate new passwords match
        if (!newPassword.equals(passwordConfirm)) {
            redirectAttributes.addFlashAttribute("error",
                    "New passwords do not match.");
            return "redirect:/profile";
        }

        // Validate new password length
        if (newPassword.isBlank() || newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error",
                    "New password must be at least 8 characters.");
            return "redirect:/profile";
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success",
                "Password changed successfully. Please log in again.");
        return "redirect:/auth/login";
    }

    @PostMapping("/security-question")
    public String setSecurityQuestion(@RequestParam String question,
                                       @RequestParam String answer,
                                       RedirectAttributes redirectAttributes) {
        if (question == null || question.isBlank()) {
            redirectAttributes.addFlashAttribute("error",
                    "Security question cannot be empty.");
            return "redirect:/profile";
        }

        if (answer == null || answer.isBlank()) {
            redirectAttributes.addFlashAttribute("error",
                    "Security answer cannot be empty.");
            return "redirect:/profile";
        }

        User user = getCurrentUser();
        user.setSecurityQuestion(question.trim());
        user.setSecurityAnswer(passwordEncoder.encode(answer.trim()));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success",
                "Security question updated successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/currency")
    public String updateCurrency(@RequestParam String currency,
                                  RedirectAttributes redirectAttributes) {
        // Validate currency code
        if (!currency.matches("^[A-Z]{3}$")) {
            redirectAttributes.addFlashAttribute("error",
                    "Invalid currency code.");
            return "redirect:/profile";
        }

        User user = getCurrentUser();
        user.setCurrencyPreference(currency);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success",
                "Currency preference updated to " + currency + ".");
        return "redirect:/profile";
    }

    @PostMapping("/delete")
    public String deleteAccount(@RequestParam String password,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();

        // Admins cannot delete their own account via this endpoint
        if (user.isAdmin() || user.isSuperAdmin()) {
            redirectAttributes.addFlashAttribute("error",
                    "Admin accounts cannot be deleted through the profile page.");
            return "redirect:/profile";
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error",
                    "Password is incorrect. Account not deleted.");
            return "redirect:/profile";
        }

        // Delete the user
        userRepository.delete(user);

        // Invalidate session
        session.invalidate();

        redirectAttributes.addFlashAttribute("success",
                "Your account has been deleted.");
        return "redirect:/";
    }
}
