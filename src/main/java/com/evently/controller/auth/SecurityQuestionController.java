package com.evently.controller.auth;

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
 * Multi-step password recovery using the user's personal security question.
 *
 * Flow:
 *   Step 1 — GET  /auth/forgot-password         → enter email
 *   Step 1 — POST /auth/forgot-password         → look up user, store email in session
 *   Step 2 — GET  /auth/forgot-password/question → show user's security question
 *   Step 2 — POST /auth/forgot-password/question → verify BCrypt answer
 *   Step 3 — GET  /auth/forgot-password/reset    → new password form
 *   Step 3 — POST /auth/forgot-password/reset    → save new password, clear session, redirect to login
 *
 * SECURITY NOTE:
 *   Each step must validate that the session holds the expected state from
 *   the previous step.  A user navigating directly to step 3 must be
 *   redirected to step 1 — never let them skip steps.
 *
 *   Use the same error message for "email not found" and "question not set"
 *   to prevent user enumeration attacks.
 *
 * OWNER: Alei
 */
@Controller
@RequestMapping("/auth/forgot-password")
@RequiredArgsConstructor
public class SecurityQuestionController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String forgotPasswordStep1() {
        return "auth/forgot-password";
    }

    @PostMapping
    public String forgotPasswordStep1Submit(@RequestParam String email,
                                             HttpSession session,
                                             RedirectAttributes redirectAttributes) {
        // Look up user by email
        var userOpt = userRepository.findByEmail(email.trim());
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Check if security question is set
            if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()) {
                // User doesn't have security question set; use same error as not found
                redirectAttributes.addFlashAttribute("error",
                        "No recovery method found. Please contact support.");
                return "redirect:/auth/forgot-password";
            }
            
            // Store user ID in session for next step
            session.setAttribute("recoveryUserId", user.getId());
            return "redirect:/auth/forgot-password/question";
        }

        // Email not found — use same message as "no security question" to prevent enumeration
        redirectAttributes.addFlashAttribute("error",
                "No recovery method found. Please contact support.");
        return "redirect:/auth/forgot-password";
    }

    @GetMapping("/question")
    public String forgotPasswordStep2(HttpSession session,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        // Guard: check if recoveryUserId is in session
        Long userId = (Long) session.getAttribute("recoveryUserId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Please start the recovery process again.");
            return "redirect:/auth/forgot-password";
        }

        // Load user and put security question in model
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            session.removeAttribute("recoveryUserId");
            redirectAttributes.addFlashAttribute("error",
                    "User not found.");
            return "redirect:/auth/forgot-password";
        }

        User user = userOpt.get();
        model.addAttribute("question", user.getSecurityQuestion());
        return "auth/forgot-password-question";
    }

    @PostMapping("/question")
    public String forgotPasswordStep2Submit(@RequestParam String answer,
                                             HttpSession session,
                                             RedirectAttributes redirectAttributes) {
        // Guard: check if recoveryUserId is in session
        Long userId = (Long) session.getAttribute("recoveryUserId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Please start the recovery process again.");
            return "redirect:/auth/forgot-password";
        }

        // Load user
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            session.removeAttribute("recoveryUserId");
            redirectAttributes.addFlashAttribute("error",
                    "User not found.");
            return "redirect:/auth/forgot-password";
        }

        User user = userOpt.get();

        // Compare answer with BCrypt hash
        if (user.getSecurityAnswer() != null &&
                passwordEncoder.matches(answer.trim(), user.getSecurityAnswer())) {
            // Correct answer — mark as verified and redirect to reset
            session.setAttribute("recoveryVerified", true);
            return "redirect:/auth/forgot-password/reset";
        }

        // Wrong answer
        redirectAttributes.addFlashAttribute("error",
                "Incorrect answer. Please try again.");
        return "redirect:/auth/forgot-password/question";
    }

    @GetMapping("/reset")
    public String forgotPasswordStep3(HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        // Guard: check if recoveryVerified is in session
        Boolean verified = (Boolean) session.getAttribute("recoveryVerified");
        if (verified == null || !verified) {
            redirectAttributes.addFlashAttribute("error",
                    "Please complete the verification steps.");
            return "redirect:/auth/forgot-password";
        }

        return "auth/forgot-password-reset";
    }

    @PostMapping("/reset")
    public String forgotPasswordStep3Submit(@RequestParam String newPassword,
                                             @RequestParam String passwordConfirm,
                                             HttpSession session,
                                             RedirectAttributes redirectAttributes) {
        // Guard: check if recoveryVerified is in session
        Boolean verified = (Boolean) session.getAttribute("recoveryVerified");
        if (verified == null || !verified) {
            redirectAttributes.addFlashAttribute("error",
                    "Please complete the verification steps.");
            return "redirect:/auth/forgot-password";
        }

        Long userId = (Long) session.getAttribute("recoveryUserId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Please start the recovery process again.");
            return "redirect:/auth/forgot-password";
        }

        // Validate passwords match
        if (!newPassword.equals(passwordConfirm)) {
            redirectAttributes.addFlashAttribute("error",
                    "Passwords do not match.");
            return "redirect:/auth/forgot-password/reset";
        }

        // Validate password is not empty and meets minimum length
        if (newPassword.isBlank() || newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error",
                    "Password must be at least 8 characters.");
            return "redirect:/auth/forgot-password/reset";
        }

        // Load user and update password
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            session.removeAttribute("recoveryUserId");
            redirectAttributes.addFlashAttribute("error",
                    "User not found.");
            return "redirect:/auth/forgot-password";
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Clear session attributes
        session.removeAttribute("recoveryUserId");
        session.removeAttribute("recoveryVerified");

        redirectAttributes.addFlashAttribute("success",
                "Password reset successful! Please log in with your new password.");
        return "redirect:/auth/login";
    }
}
